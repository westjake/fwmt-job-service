package uk.gov.ons.fwmt.job_service.controller.tm_endpoint;

import com.consiliumtechnologies.schemas.services.mobile._2009._03.messaging.SendMessageResponse;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.junit.Assert.assertNotNull;

public class GenericOutgoingWsTest {

  GenericOutgoingWs genericOutgoingWs = new GenericOutgoingWs();

  @Test
  public void sendAdapterOutput() {
    //Given

    //When
    JAXBElement<SendMessageResponse> result = genericOutgoingWs.sendAdapterOutput(null);

    //Then
    assertNotNull(result);
  }
}