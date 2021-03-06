package uk.gov.ons.fwmt.job_service.rest.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import uk.gov.ons.fwmt.job_service.exceptions.ExceptionCode;
import uk.gov.ons.fwmt.job_service.exceptions.types.FWMTCommonException;
import uk.gov.ons.fwmt.job_service.rest.client.dto.JobDto;
import uk.gov.ons.fwmt.job_service.rest.client.impl.JobResourceServiceClientImpl;

public class JobResourceServiceImplTest {

  @InjectMocks private JobResourceServiceClientImpl jobResourceServiceClient;
  @Mock private RestTemplate restTemplate;
  @Mock private ResponseEntity<JobDto> jobDtoResponseEntity;
  @Mock private ResponseEntity<Void> voidResponseEntity;
  @Mock private ResponseEntity<String> stringResponseEntity;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void existsByTmJobId() {
    //Given
    String tmJobId = "testID";
      LocalDateTime lastUpdated = LocalDateTime.parse("2018-08-01T01:06:01",DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    JobDto expectedJobDto = new JobDto(tmJobId, null, lastUpdated);
    when(restTemplate.getForEntity(any(), eq(JobDto.class), eq(tmJobId))).thenReturn(jobDtoResponseEntity);
    when(jobDtoResponseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
    when(jobDtoResponseEntity.getBody()).thenReturn(expectedJobDto);

    //When
    Boolean result = jobResourceServiceClient.existsByTmJobId(tmJobId);

    //Then
    assertTrue(result);
  }

  @Test
  public void tmJobIDExists() {
    //Given
    String tmJobId = "testID";
    LocalDateTime lastUpdated = LocalDateTime.parse("2018-08-01T01:06:01",DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    JobDto expectedJobDto = new JobDto(tmJobId, null, lastUpdated);
    when(restTemplate.getForEntity(any(), eq(JobDto.class), eq(tmJobId))).thenReturn(jobDtoResponseEntity);
    when(jobDtoResponseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
    when(jobDtoResponseEntity.getBody()).thenReturn(expectedJobDto);

    //When
    Optional<JobDto> jobDto = jobResourceServiceClient.findByTmJobId(tmJobId);

    //Then
    assertTrue(jobDto.isPresent());
  }

  @Test
  public void existsByTmJobIdAndLastAuthNo() {
    //Given
    String tmJobId = "testID";
    String lastAuthNo = "lastAuth";
    LocalDateTime lastUpdated = LocalDateTime.parse("2018-08-01T01:06:01",DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    JobDto expectedJobDto = new JobDto(tmJobId, lastAuthNo,lastUpdated);
    when(restTemplate.getForEntity(any(), eq(JobDto.class), eq(tmJobId))).thenReturn(jobDtoResponseEntity);
    when(jobDtoResponseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
    when(jobDtoResponseEntity.getBody()).thenReturn(expectedJobDto);

    //When
    boolean result = jobResourceServiceClient.existsByTmJobIdAndLastAuthNo(tmJobId, lastAuthNo);

    //Then
    assertTrue(result);
  }

  @Test
  public void doesNotExistsByTmJobIdAndLastAuthNo() {
    //Given
    String tmJobId = "testID";
    String lastAuthNo = "lastAuth";
    LocalDateTime lastUpdated = LocalDateTime.parse("2018-08-01T01:06:01",DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    JobDto expectedJobDto = new JobDto(tmJobId, lastAuthNo,lastUpdated);
    when(restTemplate.getForEntity(any(), eq(JobDto.class), eq(tmJobId)))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

    expectedException.expect(FWMTCommonException.class);
    expectedException.expectMessage(ExceptionCode.RESOURCE_SERVICE_MALFUNCTION.getCode());
    expectedException.expectMessage(HttpStatus.BAD_REQUEST.toString());

    //When
    jobResourceServiceClient.existsByTmJobIdAndLastAuthNo(tmJobId, lastAuthNo);

    //Then
    verify(restTemplate).getForEntity(any(), eq(JobDto.class), eq(tmJobId));
  }

  @Test
  public void findByTmJobId() {
    //Given
    String tmJobId = "testID";
    LocalDateTime lastUpdated = LocalDateTime.parse("2018-08-01T01:06:01",DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    JobDto expectedJobDto = new JobDto(tmJobId, null,lastUpdated);
    when(restTemplate.getForEntity(any(), eq(JobDto.class), eq(tmJobId)))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

    expectedException.expect(FWMTCommonException.class);
    expectedException.expectMessage(ExceptionCode.RESOURCE_SERVICE_MALFUNCTION.getCode());
    expectedException.expectMessage(HttpStatus.BAD_REQUEST.toString());

    //When
    Optional<JobDto> result = jobResourceServiceClient.findByTmJobId(tmJobId);

    //Then
    verify(restTemplate).getForEntity(any(), eq(JobDto.class), eq(tmJobId));
  }

  @Test
  public void createJob() {
    //Given
    String tmJobId = "testID";
    String lastAuthNo = "lastAuth";
    LocalDateTime lastUpdated = LocalDateTime.parse("2018-08-01T01:06:01",DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    JobDto jobDto = new JobDto(tmJobId, lastAuthNo,lastUpdated);
    HttpEntity<JobDto> request = new HttpEntity<>(jobDto);
    when(restTemplate.postForEntity(any(), eq(request), eq(Void.class), eq(jobDto))).thenReturn(voidResponseEntity);
    when(voidResponseEntity.getStatusCode()).thenReturn(HttpStatus.CREATED);

    //When
    jobResourceServiceClient.createJob(jobDto);

    //Then
    verify(restTemplate).postForEntity(any(), eq(request), eq(Void.class), eq(jobDto));
  }

  @Test
  public void shouldFailToCreateJobAndThrowHttpClientErrorException() {
    //Given
    String tmJobId = "testID";
    String lastAuthNo = "lastAuth";
    LocalDateTime lastUpdated = LocalDateTime.parse("2018-08-01T01:06:01",DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    JobDto jobDto = new JobDto(tmJobId, lastAuthNo,lastUpdated);
    HttpEntity<JobDto> request = new HttpEntity<>(jobDto);
    when(restTemplate.postForEntity(any(), eq(request), eq(Void.class), eq(jobDto)))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

    expectedException.expect(FWMTCommonException.class);
    expectedException.expectMessage(ExceptionCode.RESOURCE_SERVICE_MALFUNCTION.getCode());
    expectedException.expectMessage(HttpStatus.BAD_REQUEST.toString());

    //When
    jobResourceServiceClient.createJob(jobDto);

    //Then
    verify(restTemplate).postForEntity(any(), eq(request), eq(Void.class), eq(jobDto));
  }

  @Test
  public void updateJob() {
    //Given
    String tmJobId = "testID";
    String lastAuthNo = "lastAuth";
    LocalDateTime lastUpdated = LocalDateTime.parse("2018-08-01T01:06:01",DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    JobDto jobDto = new JobDto(tmJobId, lastAuthNo,lastUpdated);
    HttpEntity<JobDto> request = new HttpEntity<>(jobDto);
    doNothing().when(restTemplate).put(anyString(), any());

    //When
jobResourceServiceClient.updateJob(jobDto);

    //Then
    verify(restTemplate).put(anyString(), any());
  }

  @Test
  public void shouldFailToUpdateJobAndThrowHttpClientErrorException() {
    //Given
    String tmJobId = "testID";
    String lastAuthNo = "lastAuth";
    LocalDateTime lastUpdated = LocalDateTime.parse("2018-08-01T01:06:01",DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    JobDto jobDto = new JobDto(tmJobId, lastAuthNo, lastUpdated);
    doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST)).when(restTemplate).put(anyString(), any());

    expectedException.expect(FWMTCommonException.class);
    expectedException.expectMessage(ExceptionCode.RESOURCE_SERVICE_MALFUNCTION.getCode());
    expectedException.expectMessage(HttpStatus.BAD_REQUEST.toString());

    //When
    jobResourceServiceClient.updateJob(jobDto);

    //Then
    verify(restTemplate).put(anyString(), any());
  }

  @Test
  public void storeCSV() {
    File file = new File("bla");
    when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class))).thenReturn(stringResponseEntity);
    jobResourceServiceClient.storeCSVFile(file, true);
    verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));
  }

  @Test
  public void storeCSV4xxError() {
    File file = new File("bla");
    when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
    jobResourceServiceClient.storeCSVFile(file, true);
    verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class));
  }

}
