package com.gokulrajkn.httpstreams.controller;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import org.springframework.http.HttpHeaders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api")
@CrossOrigin(origins = "*")
public class Controller {

  public static final int BYTE_RANGE = 128;

	@Autowired
	private ResourceLoader resourceLoader;

	@CrossOrigin(origins = "*")
	@GetMapping("/withoutstream")
	public ResponseEntity<String> withoutstream() {
		StringBuilder response = new StringBuilder();
		for (int i = 1; i <= 5000; i++) {
			try {
				Thread.sleep(5);
				response.append("Line No: " + i + "\n");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return ResponseEntity.ok(response.toString());
	}

	@CrossOrigin(origins = "*")
	@GetMapping("/logs")
	public ResponseEntity<String> readFileFromResources() throws IOException {
		Resource resource = resourceLoader.getResource("classpath:" + "testlogfile.txt");
		InputStream inputStream = resource.getInputStream();
		StringBuilder stringBuilder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));) {
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line + "/n");
			}
		}
		return ResponseEntity.ok(stringBuilder.toString());
	}

	@CrossOrigin(origins = "*")
	@GetMapping("/streamlogs")
	public ResponseEntity<StreamingResponseBody> readFileFromResourcesWithStream() throws IOException {
		StreamingResponseBody responseBody = response -> {
			Resource resource = resourceLoader.getResource("classpath:" + "testlogfile.txt");
			InputStream inputStream = resource.getInputStream();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));) {
				String line;
				while ((line = reader.readLine()) != null) {
					response.write((line + "/n").getBytes());
					response.flush();
				}
			}
		};
		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(responseBody);
	}

	@CrossOrigin(origins = "*")
	@GetMapping("/stream")
	public ResponseEntity<StreamingResponseBody> stream() {
		StreamingResponseBody responseBody = response -> {
			for (int i = 1; i <= 5000; i++) {
				try {
					Thread.sleep(5);
					response.write(("Line No : " + i + "\n").getBytes());
					response.flush();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(responseBody);
	}
	
	public static final int BYTE_RANGE = 128;
	
	  @GetMapping("/stream/videos")
	  public Mono<ResponseEntity<byte[]>> streamVideo(@RequestHeader(value = "Range", required = false) String httpRangeList) {
	     return Mono.just(getContent("video1.mp4", httpRangeList, "video"));
	  }
	  @GetMapping("/audios/{fileName}")
	  public Mono<ResponseEntity<byte[]>> streamAudio(@RequestHeader(value = "Range", required = false) String httpRangeList) {
	     return Mono.just(getContent("", httpRangeList, "audio"));
	  }
	  private ResponseEntity<byte[]> getContent(String fileName, String range, String contentTypePrefix) {
	     long rangeStart = 0;
	     long rangeEnd;
	     byte[] data;
	     Long fileSize;
	     String fileType = fileName.substring(fileName.lastIndexOf(".")+1);
	     try {
	    	URL url = this.getClass().getResource("/static");
	        fileSize = Optional.ofNullable(fileName)
	              .map(file -> Paths.get(new File(url.getFile()).getAbsolutePath(), file))
	              .map(this::sizeFromFile)
	              .orElse(0L);
	        if (range == null) {
	           return ResponseEntity.status(HttpStatus.OK)
	                 .header("Content-Type", contentTypePrefix+"/" + fileType)
	                 .header("Content-Length", String.valueOf(fileSize))
	                 .body(readByteRange( fileName, rangeStart, fileSize - 1));
	        }
	        String[] ranges = range.split("-");
	        rangeStart = Long.parseLong(ranges[0].substring(6));
	        if (ranges.length > 1) {
	           rangeEnd = Long.parseLong(ranges[1]);
	        } else {
	           rangeEnd = fileSize - 1;
	        }
	        if (fileSize < rangeEnd) {
	           rangeEnd = fileSize - 1;
	        }
	        data = readByteRange( fileName, rangeStart, rangeEnd);
	     } catch (IOException e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	     }
	     String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
	     return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
	           .header("Content-Type", contentTypePrefix + "/" + fileType)
	           .header("Accept-Ranges", "bytes")
	           .header("Content-Length", contentLength)
	           .header("Content-Range", "bytes" + " " + rangeStart + "-" + rangeEnd + "/" + fileSize)
	           .body(data);
	  }
	  public byte[] readByteRange(String filename, long start, long end) throws IOException {
		 URL url = this.getClass().getResource("/static");
	     Path path = Paths.get(new File(url.getFile()).getAbsolutePath(), filename);
	     try (InputStream inputStream = (Files.newInputStream(path));
	         ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream()) {
	        byte[] data = new byte[BYTE_RANGE];
	        int nRead;
	        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
	           bufferedOutputStream.write(data, 0, nRead);
	        }
	        bufferedOutputStream.flush();
	        byte[] result = new byte[(int) (end - start) + 1];
	        System.arraycopy(bufferedOutputStream.toByteArray(), (int) start, result, 0, result.length);
	        return result;
	     }
	  }
	  private Long sizeFromFile(Path path) {
	     try {
	        return Files.size(path);
	     } catch (IOException ex) {
	        ex.printStackTrace();
	     }
	     return 0L;
	  }
}
