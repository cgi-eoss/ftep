package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.internal.UploadableFileType;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@BasePathAwareController
@RequestMapping("/filetypes")
public class FileTypeApi {

    @GetMapping(value = "/all")
    public List<UploadableFileType> getFileTypes() {
        return new ArrayList<>(Arrays.asList(UploadableFileType.values()));
    }

    @GetMapping(value="/extension/{extension}")
    public UploadableFileType getFileTypeByExtension(@PathVariable("extension") String extension) {
        return UploadableFileType.getFileTypeByExtension(extension);
    }

    @GetMapping(value="/autoDetect/{type}")
    public boolean getAutoDetectFlag(@PathVariable("type") UploadableFileType type) {
        return type.isAutoDetectFlag();
    }
}
