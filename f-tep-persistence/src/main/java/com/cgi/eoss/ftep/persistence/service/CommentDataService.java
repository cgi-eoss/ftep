package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Comment;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface CommentDataService extends FtepEntityDataService<Comment> {
    List<Comment> findByOwner(User user);
}
