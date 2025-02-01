package org.xtext.lua.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class EvalDataWriter {
	private static final String EVAL_FOLDER_PATH = "evaluation_results\\";
	private static final String MOCK_INFO_FOLDER_PATH = "evaluation_results\\mock_info_data\\";
	
	private EvalDataWriter() { }
	
	public static void writeAll(Collection<CodeModelGenerationEvalData> evalDatas) {
        Gson gson = new Gson();
        @SuppressWarnings("serial")
		var evalDatasType = new TypeToken<Collection<CodeModelGenerationEvalData>>() {}.getType();
        try (BufferedWriter writer = Files.newBufferedWriter(getCodeModelGenerationEvalDataPath())) {
        	gson.toJson(evalDatas, evalDatasType, gson.newJsonWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	private static Path getCodeModelGenerationEvalDataPath() {
		final var now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMuuuu_HHmmss"));
		final var fileName = "eval_" + now + ".json";
		return Paths.get(EVAL_FOLDER_PATH + fileName);
	}
	
	public static void writeAll(Map<MockInfo.Cause, List<SerializableMockInfo>> mockInfoMap) {
	//public static void writeAll(Map<MockInfo.Cause, List<SerializableMockInfo>> mockInfo) {
        Gson gson = new Gson();
        @SuppressWarnings("serial")
		var mockInfoMapType = new TypeToken<Map<MockInfo.Cause, List<SerializableMockInfo>>>() {}.getType();
        try (BufferedWriter writer = Files.newBufferedWriter(getMockInfoMapPath())) {
        	gson.toJson(mockInfoMap, mockInfoMapType, gson.newJsonWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	private static Path getMockInfoMapPath() {
		final var now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMuuuu_HHmmss"));
		final var fileName = "mock_info_" + now + ".json";
		return Paths.get(MOCK_INFO_FOLDER_PATH + fileName);
	}
}
