package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.Comment;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface CommentDao extends FtepEntityDao<Comment> {
    List<Comment> findByOwner(User user);
}
