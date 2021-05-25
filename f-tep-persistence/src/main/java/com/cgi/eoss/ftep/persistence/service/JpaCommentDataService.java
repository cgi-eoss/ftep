package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Comment;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.CommentDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QComment.comment;

@Service
@Transactional(readOnly = true)
public class JpaCommentDataService extends AbstractJpaDataService<Comment> implements CommentDataService {

    private final CommentDao commentDao;

    @Autowired
    public JpaCommentDataService(CommentDao commentDao) {
        this.commentDao = commentDao;
    }

    @Override
    FtepEntityDao<Comment> getDao() {
        return commentDao;
    }

    @Override
    Predicate getUniquePredicate(Comment entity) {
        return comment.owner.eq(entity.getOwner()).and(comment.creationTime.eq(entity.getCreationTime()));
    }

    @Override
    public List<Comment> findByOwner(User user) {
        return commentDao.findByOwner(user);
    }
}
