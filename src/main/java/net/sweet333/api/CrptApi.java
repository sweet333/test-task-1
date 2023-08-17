package net.sweet333.api;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final Gson gson;
    private final String url;
    private final String token;
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final Semaphore semaphore;
    private final AtomicInteger requestCount;
    private long lastRequestTime;

    public CrptApi(String token, String url, TimeUnit timeUnit, int requestLimit) {
        this.url = url;
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.token = token;
        this.requestCount = new AtomicInteger(0);
        this.semaphore = new Semaphore(1);
        this.lastRequestTime = System.currentTimeMillis();
        this.gson = new Gson();
    }

    /**
     * Создание документа
     */
    public DocumentResponseDto create(CreateDocumentDto createDocumentDto) {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastRequestTime;

        if (semaphore.tryAcquire()) {
            try {
                if (elapsedTime > timeUnit.toMillis(1)) {
                    requestCount.set(0);
                }

                if (requestCount.getAndIncrement() < requestLimit) {
                    lastRequestTime = currentTime;

                    return createDocument(createDocumentDto);
                } else {
                    throw new IllegalStateException("Request limit exceeded");
                }
            } finally {
                semaphore.release();
            }
        } else {
            throw new IllegalStateException("Cannot acquire semaphore");
        }
    }

    /**
     * Метод для создания документа без лимита
     */
    private DocumentResponseDto createDocument(CreateDocumentDto createDocumentDto) {
        String requestBody = gson.toJson(createDocumentDto);
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("content-type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), DocumentResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Дата объект для создания документа
     */
    public static class CreateDocumentDto {
        @SerializedName("product_document")
        private String productDocument;
        @SerializedName("signature")
        private String signature;
        @SerializedName("type")
        private String type;
        @SerializedName("document_format")
        private String documentFormat;

        public CreateDocumentDto(String productDocument, String signature, String type, String documentFormat) {
            this.productDocument = productDocument;
            this.signature = signature;
            this.type = type;
            this.documentFormat = documentFormat;
        }

        public CreateDocumentDto() {
        }

        public String getDocumentFormat() {
            return documentFormat;
        }

        public String getProductDocument() {
            return productDocument;
        }

        public String getSignature() {
            return signature;
        }

        public String getType() {
            return type;
        }

        public void setDocumentFormat(String documentFormat) {
            this.documentFormat = documentFormat;
        }

        public void setProductDocument(String productDocument) {
            this.productDocument = productDocument;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    /**
     * Ответ от при создании документа
     */
    public static class DocumentResponseDto {
        @SerializedName("value")
        private String value;

        public DocumentResponseDto(String value) {
            this.value = value;
        }

        public DocumentResponseDto() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
