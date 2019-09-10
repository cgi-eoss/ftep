package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.ApiKey;
import com.cgi.eoss.ftep.persistence.service.ApiKeyDataService;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.DatatypeConverter;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * <p>Functionality for generating F-TEP API keys</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/apiKeys")
@Log4j2
public class ApiKeysApi {

    private final FtepSecurityService ftepSecurityService;
    private final ApiKeyDataService apiKeyDataService;

    @Autowired
    public ApiKeysApi(ApiKeyDataService apiKeyDataService, FtepSecurityService ftepSecurityService) {
        this.apiKeyDataService = apiKeyDataService;
        this.ftepSecurityService = ftepSecurityService;
    }

    @PostMapping("/generate")
    public ResponseEntity generate(PersistentEntityResourceAssembler resourceAssembler) {
        if (apiKeyDataService.getByOwner(ftepSecurityService.getCurrentUser()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        String apiKeyString = generateApiKey(256);
        String encryptedApiKeyString;
        try {
            encryptedApiKeyString = encryptApiKey(apiKeyString);
        } catch (NoSuchAlgorithmException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        ApiKey apiKey = new ApiKey(encryptedApiKeyString);
        ftepSecurityService.updateOwnerWithCurrentUser(apiKey);
        apiKeyDataService.save(apiKey);
        return ResponseEntity.created(URI.create(resourceAssembler.getSelfLinkFor(apiKey).getHref())).body(apiKeyString);
    }

    @GetMapping("/exists")
    public ResponseEntity exists() {
        ApiKey apiKey = apiKeyDataService.getByOwner(ftepSecurityService.getCurrentUser());
        return ResponseEntity.ok(apiKey != null);
    }

    @DeleteMapping("/delete")
    public ResponseEntity delete() {
        ApiKey apiKey = apiKeyDataService.getByOwner(ftepSecurityService.getCurrentUser());
        if (apiKey != null) {
            apiKeyDataService.delete(apiKey);
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/regenerate")
    public ResponseEntity regenerate() {
        ApiKey apiKey = apiKeyDataService.getByOwner(ftepSecurityService.getCurrentUser());
        if (apiKey != null) {
            String apiKeyString = generateApiKey(256);
            String encryptedApiKeyString;
            try {
                encryptedApiKeyString = encryptApiKey(apiKeyString);
            } catch (NoSuchAlgorithmException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            apiKey.setApiKeyString(encryptedApiKeyString);
            apiKeyDataService.save(apiKey);
            return ResponseEntity.ok(apiKeyString);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private String generateApiKey(final int keyLen) {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[keyLen / 8];
        random.nextBytes(bytes);
        return DatatypeConverter.printHexBinary(bytes).toLowerCase();
    }

    private String encryptApiKey(String password) throws NoSuchAlgorithmException {
        return "{SHA}" + new String(Base64.getEncoder().encode(java.security.MessageDigest.getInstance("SHA1").digest(password.getBytes())));
    }

}
