package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepGroup;
import com.cgi.eoss.ftep.model.FtepUser;

import java.util.List;

public interface FtepGroupDao extends FtepEntityDao<FtepGroup> {
    List<FtepGroup> findByNameContainingIgnoreCase(String term);

    List<FtepGroup> findByMembersContaining(FtepUser member);

    List<FtepGroup> findByOwner(FtepUser user);
}
