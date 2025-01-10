package org.xtext.lua.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class EvalDataWriter {
	private static final String EVAL_FOLDER_PATH = "evaluation_results\\";
	
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
}
