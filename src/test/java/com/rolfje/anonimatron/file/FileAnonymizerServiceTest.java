package com.rolfje.anonimatron.file;

import com.rolfje.anonimatron.anonymizer.*;
import com.rolfje.anonimatron.configuration.Column;
import com.rolfje.anonimatron.configuration.Configuration;
import com.rolfje.anonimatron.configuration.DataFile;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class FileAnonymizerServiceTest extends TestCase {

	private FileAnonymizerService fileAnonymizerService;

	public void setUp() throws Exception {
		super.setUp();

		Configuration config = new Configuration();
		AnonymizerService anonymizerService = new AnonymizerService();
		anonymizerService.registerAnonymizers(config.getAnonymizerClasses());
		fileAnonymizerService = new FileAnonymizerService(config, anonymizerService);
	}

	public void testAnonymize() throws Exception {
		Record record = new Record(new String[]{new DutchBSNAnononymizer().getType()}, new String[]{"ABC"});
		Record anonymize = fileAnonymizerService.anonymize(record);

		assertEquals(record.types[0], anonymize.types[0]);
		assertFalse(record.values[0].equals(anonymize.values[0]));
	}

	public void testAnonymizeRecords() throws Exception {
		String[] types = new String[]{
				new DutchBSNAnononymizer().getType(),
				new StringAnonymizer().getType()
		};

		final List<Record> sourceRecords = new ArrayList<Record>();

		for (int i = 0; i < 10; i++) {
			Object[] values = new Object[]{
					"MyBSN",
					"some private text"
			};
			sourceRecords.add(new Record(types, values));
		}

		RecordReader recordReader = new RecordReader() {
			@Override
			public void close() throws IOException {
				// nothing
			}

			private int i = 0;


			@Override
			public boolean hasRecords() {
				return i < sourceRecords.size();
			}

			@Override
			public Record read() {
				Record record = sourceRecords.get(i);
				i = i + 1;
				return record;
			}
		};

		final List<Record> targetRecords = new ArrayList<Record>();
		RecordWriter recordWriter = new RecordWriter() {
			@Override
			public void close() throws IOException {
				// nothing
			}

			@Override
			public void write(Record record) {
				targetRecords.add(record);
			}
		};

		fileAnonymizerService.anonymize(recordReader, recordWriter);
		assertEquals(sourceRecords.size(), targetRecords.size());

		for (int i = 0; i < sourceRecords.size(); i++) {
			Record source = sourceRecords.get(i);
			Record target = targetRecords.get(i);

			for (int j = 0; j < source.types.length; j++) {
				assertEquals(source.types[j], target.types[j]);
				assertFalse(source.values[j].equals(target.types[j]));
				assertNotNull(target.types[j]);
			}
		}
	}

	public void testIntegrationTest() throws Exception {
		// Create testfile
		File tempInput = File.createTempFile("tempInput", ".csv");
		PrintWriter printWriter = new PrintWriter(tempInput);
		printWriter.write("test1,test2,test3\n");
		printWriter.write("test3,test2,test1\n");
		printWriter.close();


		String tempOutput = tempInput.getAbsoluteFile() + ".out.csv";

		List<Column> columns = Arrays.asList(
				new Column[]{
						new Column("1", "String")
				});

		DataFile dataFile = new DataFile();
		dataFile.setInFile(tempInput.getAbsolutePath());
		dataFile.setOutFile(tempOutput);
		dataFile.setReader(CsvFileReader.class.getCanonicalName());
		dataFile.setWriter(CsvFileWriter.class.getCanonicalName());
		dataFile.setColumns(columns);

		Configuration configuration = new Configuration();
		configuration.setFiles(Arrays.asList(new DataFile[]{dataFile}));
		AnonymizerService anonymizerService = new AnonymizerService();
		anonymizerService.registerAnonymizers(configuration.getAnonymizerClasses());
		fileAnonymizerService = new FileAnonymizerService(configuration, anonymizerService);

		fileAnonymizerService.anonymize();

		File outPutFile = new File(tempOutput);
		assertTrue(outPutFile.exists());

		CsvFileReader csvFileReader = new CsvFileReader(outPutFile.getAbsoluteFile());
		Record outputRecord1 = csvFileReader.read();
		Record outputRecord2 = csvFileReader.read();
		assertFalse(csvFileReader.hasRecords());

		assertEquals(outputRecord1.values[0], outputRecord2.values[2]);
		assertEquals(outputRecord1.values[1], outputRecord2.values[1]);
		assertEquals(outputRecord1.values[2], outputRecord2.values[0]);
	}
}