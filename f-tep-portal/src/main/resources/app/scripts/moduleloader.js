define([
    //contollers

    './controllers/index',
    './controllers/navbar',

    './controllers/account/account',

    './controllers/common/messages',
    './controllers/common/bottombar/bottombar',
    './controllers/common/bottombar/databasket',
    './controllers/common/bottombar/jobs',
    './controllers/common/bottombar/results',

    './controllers/community/community',
    './controllers/community/manage/manage',
    './controllers/community/manage/databaskets/databasketscontainer',
    './controllers/community/manage/databaskets/databasket',
    './controllers/community/manage/databaskets/databaskets',
    './controllers/community/manage/groups/groupscontainer',
    './controllers/community/manage/groups/group',
    './controllers/community/manage/groups/groups',
    './controllers/community/manage/groups/users',
    './controllers/community/manage/jobs/jobscontainer',
    './controllers/community/manage/jobs/job',
    './controllers/community/manage/jobs/jobs',
    './controllers/community/manage/projects/projectscontainer',
    './controllers/community/manage/projects/project',
    './controllers/community/manage/projects/projects',
    './controllers/community/manage/services/servicescontainer',
    './controllers/community/manage/services/service',
    './controllers/community/manage/services/services',
    './controllers/community/share/share',

    './controllers/developer/developer',
    './controllers/developer/service',

    './controllers/explorer/explorer',
    './controllers/explorer/map',
    './controllers/explorer/sidebar/project',
    './controllers/explorer/sidebar/search',
    './controllers/explorer/sidebar/services',
    './controllers/explorer/sidebar/sidebar',
    './controllers/explorer/sidebar/workspace',

    './controllers/helpdesk/helpdesk',

    //services
    './services/basketservice',
    './services/commonservice',
    './services/fileservice',
    './services/geoservice',
    './services/groupservice',
    './services/jobservice',
    './services/mapservice',
    './services/messageservice',
    './services/projectservice',
    './services/productservice',
    './services/referenceservice',
    './services/tabservice',
    './services/userservice',
    './services/wpsservice'

], function () {});
