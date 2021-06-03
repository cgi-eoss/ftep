package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Comment;
import com.cgi.eoss.ftep.model.QComment;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.Subscription;
import com.cgi.eoss.ftep.persistence.dao.CommentDao;
import com.cgi.eoss.ftep.persistence.dao.SubscriptionDao;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class CommentsApiCustomImpl extends BaseRepositoryApiImpl<Comment> implements CommentsApiCustom {

    private final FtepSecurityService securityService;
    private final CommentDao dao;

    public CommentsApiCustomImpl(FtepSecurityService securityService, CommentDao commentDao) {
        super(Comment.class);
        this.securityService = securityService;
        this.dao = commentDao;
    }

    @Override
    NumberPath<Long> getIdPath() {
        return QComment.comment.id;
    }

    @Override
    QUser getOwnerPath() {
        return QComment.comment.owner;
    }

    @Override
    Class<Comment> getEntityClass() {
        return Comment.class;
    }
}
