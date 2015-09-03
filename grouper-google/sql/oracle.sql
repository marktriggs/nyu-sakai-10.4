create table grouper_status (setting varchar2(255) PRIMARY KEY, value varchar2(255));

create table grouper_groups (group_id varchar2(255) PRIMARY KEY, sakai_group_id varchar2(255), description varchar2(512));
create index grouper_groups_sakai_id on grouper_groups (sakai_group_id);

create table grouper_group_users (group_id varchar2(255), netid varchar2(255), role varchar2(30),
       PRIMARY KEY (group_id, netid),
       FOREIGN KEY (group_id) references grouper_groups (group_id));

create index grouper_groups_role on grouper_group_users (role);

create view grouper_memberships as select group_id, netid from grouper_group_users where role = 'viewer';
create view grouper_managers as select group_id, netid from grouper_group_users where role = 'manager';
