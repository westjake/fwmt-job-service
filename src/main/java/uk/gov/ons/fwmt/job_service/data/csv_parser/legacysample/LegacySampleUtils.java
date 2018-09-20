package uk.gov.ons.fwmt.job_service.data.csv_parser.legacysample;

import org.apache.commons.csv.CSVRecord;
import uk.gov.ons.fwmt.job_service.data.legacy_ingest.LegacySampleIngest;
import uk.gov.ons.fwmt.job_service.data.legacy_ingest.LegacySampleLFSDataIngest;
import uk.gov.ons.fwmt.job_service.data.legacy_ingest.LegacySampleSurveyType;
import uk.gov.ons.fwmt.job_service.exceptions.types.FWMTCommonException;
import uk.gov.ons.fwmt.job_service.rest.client.FieldPeriodResourceServiceClient;
import uk.gov.ons.fwmt.job_service.rest.client.dto.FieldPeriodDto;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Optional;

public final class LegacySampleUtils {
  private LegacySampleUtils() {}
  
  /**
   * Create a unique Job ID that can be used by TotalMobile from existing fields
   * within the CSV The method varies on the type of survey
   */
  public static String constructTmJobId(CSVRecord record, LegacySampleSurveyType surveyType) {
    switch (surveyType) {
    case GFF:
      // quota '-' addr '-' stage
      return record.get("TLA") + "-" + record.get("Quota") + "-" + record.get("AddressNo") + "-" + record.get("Stage");
    case LFS:
      // quota week w1yr qrtr addr wavfnd hhld chklet
      if (!record.get("Issue_No").equals("1")) {
        return record.get("QUOTA") + " " + record.get("WEEK") + " " + record.get("W1YR") + " " + record.get("QRTR")
            + " "
            + record.get("ADDR")
            + " " + record.get("WAVFND") + " " + record.get("HHLD") + " " + record.get("CHKLET") + " - "
            + record.get("FP") + " [R" + record.get("Issue_No") + "]";
      } else {
        return record.get("QUOTA") + " " + record.get("WEEK") + " " + record.get("W1YR") + " " + record.get("QRTR")
            + " "
            + record.get("ADDR")
            + " " + record.get("WAVFND") + " " + record.get("HHLD") + " " + record.get("CHKLET") + " - "
            + record.get("FP");
      }
    default:
      throw new IllegalArgumentException("Invalid survey type");
    }
  }

  
  public static LocalDate convertToFieldPeriodDate(String stage, FieldPeriodResourceServiceClient fieldPeriodResourceServiceClient) throws FWMTCommonException{
    final Optional<FieldPeriodDto> existsByFieldperiod = fieldPeriodResourceServiceClient.findByFieldPeriod(stage);
    if (existsByFieldperiod.isPresent()) {
      final FieldPeriodDto fieldPeriod = existsByFieldperiod.get();
      return fieldPeriod.getEndDate();
    } else {
      throw FWMTCommonException.makeUnknownFieldPeriodException(stage);
    }
  }

  public static LegacySampleLFSDataIngest checkSetLookingForWorkIndicator (LegacySampleIngest instance, CSVRecord record) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    String workIndicator;
    String jbaway;
    String ownbus;
    String relbus;
    String look4;
    String difJob;

    System.out.println(record);

    LegacySampleLFSDataIngest lfs = instance.getLfsData();
    Class lfsClass =lfs.getClass();

    Method workIndicatorMethod;
    Method look4WorkMethod;

    for (int i = 1; i <= 16; i++) {

      workIndicator = record.get("QINDIV_" + i + "_WRKING");
      jbaway = record.get("QINDIV_" + i + "_JBAWAY");
      ownbus = record.get("QINDIV_" + i + "_OWNBUS");
      relbus = record.get("QINDIV_" + i + "_RELBUS");
      look4 = record.get("QINDIV_" + i + "_LOOK4");
      difJob = record.get("QINDIV_" + i + "_DIFJOB");

      try {
        workIndicatorMethod = lfsClass.getDeclaredMethod("setRespondentWorkIndicator" + i, String.class);
        look4WorkMethod = lfsClass.getDeclaredMethod("setRespondentLookingForWork" + i, String.class);
      } catch (NoSuchMethodException e) {
        throw e;
      }

      try {
        if (workIndicator.equals("1") || jbaway.equals("1") || ownbus.equals("1") || relbus.equals("1")) {
          workIndicatorMethod.invoke(lfs, "W");
        } else {
          workIndicatorMethod.invoke(lfs,"N");
        }

        if (look4.equals("1") || difJob.equals("1")) {
          look4WorkMethod.invoke(lfs,"L");
        } else {
          look4WorkMethod.invoke(lfs,"N");
        }
      }
       catch(IllegalAccessException e){
        throw e;
      } catch(InvocationTargetException e) {
        throw e;
      }
    }
    return lfs;
  }
}
