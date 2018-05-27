package uk.gov.ons.fwmt.job_service.service.impl;

import com.consiliumtechnologies.schemas.services.mobile._2009._03.messaging.SendCreateJobRequestMessage;
import com.consiliumtechnologies.schemas.services.mobile._2009._03.messaging.SendUpdateJobHeaderRequestMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.ons.fwmt.job_service.data.csv_parser.CSVParseResult;
import uk.gov.ons.fwmt.job_service.data.csv_parser.UnprocessedCSVRow;
import uk.gov.ons.fwmt.job_service.data.dto.SampleSummaryDTO;
import uk.gov.ons.fwmt.job_service.data.file_ingest.FileIngest;
import uk.gov.ons.fwmt.job_service.data.legacy_ingest.LegacySampleIngest;
import uk.gov.ons.fwmt.job_service.entity.TMJobEntity;
import uk.gov.ons.fwmt.job_service.entity.TMUserEntity;
import uk.gov.ons.fwmt.job_service.error.InvalidFileNameException;
import uk.gov.ons.fwmt.job_service.error.MediaTypeNotSupportedException;
import uk.gov.ons.fwmt.job_service.repo.TMJobRepo;
import uk.gov.ons.fwmt.job_service.repo.TMUserRepo;
import uk.gov.ons.fwmt.job_service.service.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class LegacyServiceImpl implements LegacyService {
  private FileIngestService fileIngestService;
  private CSVParsingService csvParsingService;
  private TMJobConverterService tmJobConverterService;
  private TMService tmService;
  private TMUserRepo tmUserRepo;
  private TMJobRepo tmJobRepo;

  @Autowired
  public LegacyServiceImpl(
      FileIngestService fileIngestService,
      CSVParsingService csvParsingService,
      TMJobConverterService tmJobConverterService,
      TMService tmService,
      TMUserRepo tmUserRepo,
      TMJobRepo tmJobRepo
  ) {
    this.fileIngestService = fileIngestService;
    this.csvParsingService = csvParsingService;
    this.tmJobConverterService = tmJobConverterService;
    this.tmService = tmService;
    this.tmUserRepo = tmUserRepo;
    this.tmJobRepo = tmJobRepo;
  }

  protected Optional<UnprocessedCSVRow> sendJobToUser(int row, LegacySampleIngest ingest, TMUserEntity userEntity) {
    String authno = userEntity.getAuthNo();
    String username = userEntity.getTmUsername();
    try {
      if (tmJobRepo.existsByTmJobIdAndLastAuthNo(ingest.getTmJobId(), authno)) {
        log.info("Job has been sent previously");
        return Optional.of(new UnprocessedCSVRow(row, "Job has been sent previously"));
      } else if (tmJobRepo.existsByTmJobId(ingest.getTmJobId())) {
        log.info("Job is a reallocation");
        SendUpdateJobHeaderRequestMessage request = tmJobConverterService.updateJob(ingest, username);
        // TODO add error handling
        tmService.send(request);
      } else {
        switch (ingest.getLegacySampleSurveyType()) {
        case GFF:
          if (ingest.isGffReissue()) {
            log.info("Job is a GFF reissue");
            // send the job to TM
            SendCreateJobRequestMessage request = tmJobConverterService.createReissue(ingest, username);
            tmService.send(request);
            // update the last auth no in the database
            TMJobEntity jobEntity = tmJobRepo.findByTmJobId(ingest.getTmJobId());
            jobEntity.setLastAuthNo(authno);
            tmJobRepo.save(jobEntity);
          } else {
            log.info("Job is a new GFF job");
            // send the job to TM
            SendCreateJobRequestMessage request = tmJobConverterService.createJob(ingest, username);
            tmService.send(request);
            // save the job in the database
            tmJobRepo.save(new TMJobEntity(ingest.getTmJobId(), username));
          }
          break;
        case LFS:
          log.info("Job is a new LFS job");
          // send the job to TM
          SendCreateJobRequestMessage request = tmJobConverterService.createJob(ingest, username);
          tmService.send(request);
          // save the job in the database
          tmJobRepo.save(new TMJobEntity(ingest.getTmJobId(), username));
          break;
        default:
          throw new IllegalArgumentException("Unknown survey type");
        }
      }
      log.info("Job sent successfully");
      return Optional.empty();
    } catch (Exception e) {
      log.error("Error while sending job", e);
      return Optional.of(new UnprocessedCSVRow(row, e.toString()));
    }
  }

  protected Optional<TMUserEntity> findUser(LegacySampleIngest ingest) {
    log.info("Handling entry with authno: " + ingest.getAuth());
    TMUserEntity entity = tmUserRepo.findByAuthNo(ingest.getAuth());
    if (entity != null) {
      log.info("Found user by authno: " + entity.toString());
      return Optional.of(entity);
    }
    entity = tmUserRepo.findByAlternateAuthNo(ingest.getAuth());
    if (entity != null) {
      log.info("Found user by alternate authno: " + entity.toString());
      return Optional.of(entity);
    } else {
      return Optional.empty();
    }
  }

  @Override public SampleSummaryDTO processSampleFile(MultipartFile file)
      throws IOException, InvalidFileNameException, MediaTypeNotSupportedException {
    FileIngest fileIngest = fileIngestService.ingestSampleFile(file);
    Iterator<CSVParseResult<LegacySampleIngest>> csvRowIterator = csvParsingService
        .parseLegacySample(fileIngest.getReader(), fileIngest.getFilename().getTla());

    int parsed = 0;
    List<UnprocessedCSVRow> unprocessed = new ArrayList<>();

    while (csvRowIterator.hasNext()) {
      CSVParseResult<LegacySampleIngest> row = csvRowIterator.next();
      if (row.isError()) {
        log.info("Entry could not be processed");
        unprocessed.add(new UnprocessedCSVRow(row.getRow(), "Row could not be parsed: " + row.getErrorMessage()));
        continue;
      }
      LegacySampleIngest ingest = row.getResult();
      Optional<TMUserEntity> user = findUser(ingest);
      if (!user.isPresent()) {
        log.info("User did not exist in the gateway");
        unprocessed.add(new UnprocessedCSVRow(row.getRow(), "User did not exist in the gateway: " + ingest.getAuth()));
        continue;
      }
      if (!user.get().isActive()) {
        log.info("User was not active");
        unprocessed.add(new UnprocessedCSVRow(row.getRow(), "User was not active: " + ingest.getAuth()));
        continue;
      }
      Optional<UnprocessedCSVRow> unprocessedCSVRow = sendJobToUser(row.getRow(), ingest, user.get());
      if (unprocessedCSVRow.isPresent()) {
        log.info("Job could not be sent");
        unprocessed.add(unprocessedCSVRow.get());
        continue;
      }
      parsed++;
    }

    // construct reply
    return new SampleSummaryDTO(file.getOriginalFilename(), parsed, unprocessed);
  }

}
