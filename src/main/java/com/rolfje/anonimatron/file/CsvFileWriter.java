package com.rolfje.anonimatron.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CsvFileWriter implements RecordWriter {

	private BufferedWriter writer;

	public CsvFileWriter(String fileName) throws IOException {
		this(new File(fileName));
	}

	public CsvFileWriter(File file) throws IOException {
		writer = new BufferedWriter(new FileWriter(file));
	}

	@Override
	public void write(Record record) {
		StringBuilder line = new StringBuilder();

		for (int i = 0; i < record.values.length; i++) {
			String value = record.values[i].toString();
			line.append(value);
			if (i < record.values.length - 1) {
				line.append(",");
			}
		}

		try {
			writer.write(line.toString() + "\n");
		} catch (IOException e) {
			throw new RuntimeException("Problem writing file.", e);
		}

	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
