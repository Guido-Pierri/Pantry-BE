package com.guidopierri.pantrybe.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guidopierri.pantrybe.config.EntityMapper;
import com.guidopierri.pantrybe.dtos.responses.DabasItemResponse;
import com.guidopierri.pantrybe.exceptions.DabasException;
import com.guidopierri.pantrybe.models.DabasItem;
import com.guidopierri.pantrybe.models.SearchParams;
import com.guidopierri.pantrybe.models.dabas.search.Search;
import com.guidopierri.pantrybe.repositories.DabasItemRepository;
import com.guidopierri.pantrybe.services.search.DabasItemSearchSpecification;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DabasDataService implements DataProvider {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DabasDataService.class);
    private final ItemService itemService;
    private final DabasItemRepository dabasItemRepository;
    private final EntityMapper entityMapper;
    private final EntityManager entityManager;

    public DabasDataService(ItemService itemService, DabasItemRepository dabasItemRepository, EntityMapper entityMapper, EntityManager entityManager) {
        this.itemService = itemService;
        this.dabasItemRepository = dabasItemRepository;
        this.entityMapper = entityMapper;
        this.entityManager = entityManager;
    }

    private Pageable createPageRequestUsing(int page, int size) {
        return PageRequest.of(page, size);
    }

    @Override
    public Optional<DabasItemResponse> getArticle(String gtinNumber) throws Exception {
        String url = "https://api.dabas.com/DABASService/V2/article/gtin/" + gtinNumber + "/JSON?apikey=741ffd2b-3be4-49b8-b837-45be48c7e7be";
        Optional<String> response = sendApiRequest(url);


        if (response.isEmpty()) {
            log.error("404 Error: Resource not found for search parameter: {}", gtinNumber);
            // or handle it in another way based on your requirements
        }
        if (response.isEmpty()) {
            return Optional.empty();
        }
        try {
            JsonNode jsonNode = validateResponse(response.get());
            String productName = jsonNode.path("Produktnamn").asText();
            String gtin = jsonNode.path("GTIN").asText();
            String brand = jsonNode.path("Varumarke").path("Varumarke").asText();
            String mainGroup = jsonNode.path("Varugrupp").path("HuvudgruppBenamning").asText();
            JsonNode bilder = jsonNode.path("Bilder");
            String imageLink = (bilder.isArray() && !bilder.isEmpty()) ? bilder.path(0).path("Lank").asText() : null;
            String size = jsonNode.path("Storlek").asText();
            String ingredients = jsonNode.path("Ingrediensforteckning").asText();
            ingredients = ingredients.replace("\r", "").replace("\n", "").replace("+", "").replace("*", "");
            String productClassifications = jsonNode.path("Produktkod").asText();
            String bruteWeight = jsonNode.path("Nettoinnehall").asText();
            String drainedWeightUnit = jsonNode.path("MangdFardigVaraEnhetKod").asText();
            String drainedWeight = jsonNode.path("MangdFardigVara_Formatted").asText() + " " + drainedWeightUnit;

            log.info("productName: {}", productName);
            log.info("gtin: {}", gtin);
            log.info("brand: {}", brand);
            log.info("imageLink: {}", imageLink);
            log.info("mainGroup: {}", mainGroup);
            log.info("size: {}", size);
            log.info("ingredients: {}", ingredients);
            log.info("productClassifications: {}", productClassifications);
            log.info("bruteWeight: {}", bruteWeight);
            log.info("drainedWeight: {}", drainedWeight);

            return Optional.of(new DabasItemResponse(gtin,
                    productName,
                    brand,
                    imageLink,
                    mainGroup,
                    size,
                    ingredients,
                    productClassifications,
                    bruteWeight,
                    drainedWeight));

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private JsonNode validateResponse(String response) throws JsonProcessingException {
        if (response == null || response.isEmpty()) {
            throw new RuntimeException("Error processing SIBS data: Response is empty");
        }
        JsonNode jsonNode = getObjectMapper().readTree(response);
        if (jsonNode.isEmpty()) {
            return jsonNode;
        }
        return jsonNode;
    }

    @Override
    public List<Search> fetchUpaginatedSearch(String searchParameter) {
        List<Search> searchList = new ArrayList<>();
        String url = "https://api.dabas.com/DABASService/V2/articles/basesearchparameter/" + searchParameter + "/JSON?apikey=741ffd2b-3be4-49b8-b837-45be48c7e7be";
        String jsonString;

        try {
            jsonString = (sendApiRequest(url).get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Check for 404 error
        if (jsonString.contains("Not Found")) {
            System.out.println("404 Error: Resource not found for search parameter: " + searchParameter);
            return searchList; // or handle it in another way based on your requirements
        }
        System.out.println("jsonString:" + jsonString);
        try {
            return Arrays.stream(getObjectMapper().readValue(jsonString, Search[].class)).toList();

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    Optional<String> sendApiRequest(String url) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/json")
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        log.info("HTTP request took {} milliseconds", duration);

        int statusCode = response.statusCode();
        if (statusCode == 404) {
            log.error("404 Error: Resource not found: {}", url);
            return Optional.empty();
            // Handle it as needed

        }
        return response.body().describeConstable();
    }

    @Override
    @Cacheable(value = "search", key = "{#searchParameter, #page}")
    public Page<DabasItemResponse> searchToPageable(String searchParameter, int page, int size) throws Exception {
        Pageable pageRequest = createPageRequestUsing(page, size);

        List<Search> searchList = fetchUpaginatedSearch(searchParameter);
        log.info("searchList: {}", searchList);
        int pageSize = pageRequest.getPageSize();
        int start = (int) pageRequest.getOffset();
        int end = Math.min((start + pageSize), searchList.size());
        List<DabasItemResponse> dtos = searchList.parallelStream()
                .map(search -> {
                    var item = getArticleByGtin(search.getGtin());
                    if (item.isEmpty()) {
                        return new DabasItemResponse(search.getGtin(), search.getArtikelbenamning(), search.getVarumarke(), null, search.getArtikeltyp(), null, null, null, null, null);
                    }
                    String imageLink = item.get().getImage();
                    return new DabasItemResponse(search.getGtin(), search.getArtikelbenamning(), search.getVarumarke(), imageLink, search.getArtikeltyp(), null, null, null, null, null);

                })
                .collect(Collectors.toList());
        List<DabasItemResponse> pageContent = dtos.subList(start, end);
        return new PageImpl<>(pageContent, pageRequest, dtos.size());
    }

    private Optional<DabasItem> getArticleByGtin(String gtin) {
        return dabasItemRepository.findDabasItemByGtin(gtin);
    }

    public List<DabasItem> sanitize() {

        List<DabasItem> allItems = dabasItemRepository.findAll();

        // Group the articles by GTIN and identify the duplicates
        Map<String, List<DabasItem>> groupedByGtin = allItems.stream()
                .collect(Collectors.groupingBy(DabasItem::getGtin));
        for (List<DabasItem> duplicates : groupedByGtin.values()) {
            if (duplicates.size() > 1) {
                // Keep the first article and get a list of the rest
                List<DabasItem> toDelete = duplicates.subList(1, duplicates.size());

                // Delete the duplicates
                dabasItemRepository.deleteAllInBatch(toDelete);
            }
        }
        log.info("All items: {}", allItems.size());
        log.info("Grouped by GTIN: {}", groupedByGtin.size());
        log.info("Duplicates: {}", allItems.size() - groupedByGtin.size());
        // Now, duplicates list contains all the duplicate items
        // You can delete them from the database
        // Return the list of unique items
        allItems = dabasItemRepository.findAll();
        log.info("All items: {}", allItems.size());
        return allItems;
    }

    public void saveAll(List<DabasItem> users) {
        dabasItemRepository.saveAllAndFlush(users);
    }

    public void seedArticles() {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<List<DabasItem>> typeReference = new TypeReference<>() {
        };
        InputStream inputStream = TypeReference.class.getResourceAsStream("/dabas-items/dabasItems.json");
        try {
            List<DabasItem> items = mapper.readValue(inputStream, typeReference);
            log.info("Nr of items seeded: {}", items.size());
            saveAll(items.subList(0, 1999));
            log.info("Items Saved!");
        } catch (IOException e) {
            log.info("Unable to save users: {}", e.getMessage());
        }
        //entityMapper.convertListOfEmployeeToListOfEmployeeResponse(dabasItemRepository.findAll());
    }

    @Scheduled(cron = "0 2 * * 0") // run every week at 2:00 AM
    public List<DabasItemResponse> importArticlesGtin() throws Exception {
        List<Map<String, String>> articlesMap = new ArrayList<>();
        String apikey = "741ffd2b-3be4-49b8-b837-45be48c7e7be";
        String url = "https://api.dabas.com/DABASService/V2/articles/JSON?apikey=" + apikey;
        String jsonString;

        jsonString = (sendApiRequest(url).orElseThrow(() ->
                new DabasException("Error: No response from DABAS")));

        log.info("jsonString: {}", jsonString);
        int articlesCount = 0;
        for (Map map : getObjectMapper().readValue(jsonString, Map[].class)) {
            Map<String, String> article = new HashMap<>();
            article.put("GTIN", map.get("GTIN").toString());
            articlesMap.add(article);
            articlesCount++;
        }
        log.info("Found {} Articles in DABAS ", articlesCount);

        int batchSize = 100; // Define your batch size here
        List<DabasItemResponse> articles = new ArrayList<>();
        for (int i = 0; i < articlesMap.size(); i += batchSize) {
            int end = Math.min(i + batchSize, articlesMap.size());
            List<Map<String, String>> batch = articlesMap.subList(i, end);

            for (Map<String, String> article : batch) {
                Optional<DabasItemResponse> response = getArticle(article.get("GTIN"));
                if (response.isPresent()) {
                    dabasItemRepository.saveAndFlush(entityMapper.dabasItemResponseToDabasItem(response.get()));
                    articles.add(response.get());
                    log.info("Imported article: {}", response.get().name());
                    log.info("article nr: {} from :{}", articles.size(), articlesCount);
                }
            }
        }
        return articles;
    }

    private List<DabasItemResponse> getEmployeeByIds(List<Long> employeeIds) {
        return entityMapper.convertListOfEmployeeToListOfEmployeeResponse(dabasItemRepository.findAllById(employeeIds));
    }

    public List<DabasItemResponse> search(SearchParams search, Pageable pageable) {
        Page<Long> page = findIdsBySpecification(new DabasItemSearchSpecification(search), pageable, DabasItem.class);
        return getEmployeeByIds(page.getContent());
    }


    public <T> Page<Long> findIdsBySpecification(Specification<T> specification,
                                                 Pageable pageable, Class<T> clazz) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<T> root = query.from(clazz);
        query.select(root.get("id"))
                .where(specification.toPredicate(root, query, builder))
                .orderBy(builder.asc(root.get(DabasItemSearchSpecification.NAME)));

        var result = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());

        CriteriaBuilder countBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = countBuilder.createQuery(Long.class);
        Root<T> countRoot = countQuery.from(clazz);
        countQuery.select(countBuilder.count(countRoot))
                .where(specification.toPredicate(countRoot, countQuery, countBuilder));

        var totalElements = entityManager.createQuery(countQuery).getSingleResult();
        return new PageImpl<>(result.getResultList(), pageable, totalElements);
    }

    public void checkAndImportArticles() throws Exception {
        if (dabasItemRepository.count() == 0) {
            importArticlesGtin();
        }
    }

    public void checkAndSeedArticles() {
        if (dabasItemRepository.count() == 0) {
            seedArticles();
        }
    }

    @PostConstruct
    public void init() {
        checkAndSeedArticles();
        //checkAndImportArticles();

    }
}