package uk.gov.ons.fwmt.job_service.data.csv_parser;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CSVParseFinalResult {
  private final List<UnprocessedCSVRow> unprocessedCSVRows;
  private final int parsedCount;
  private final int unparsedCount;
}
