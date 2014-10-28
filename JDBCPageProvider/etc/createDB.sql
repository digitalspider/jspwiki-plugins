CREATE USER jspwiki WITH PASSWORD 'jspwiki';
CREATE DATABASE jspwiki;
GRANT ALL PRIVILEGES ON DATABASE jspwiki to jspwiki;

create table jspwiki (
  id SERIAL PRIMARY KEY,
  name varchar(255) NOT NULL,
  version int NOT NULL DEFAULT -1,
  text text NULL,
  author varchar(255) NOT NULL,
  changenote varchar(255) NULL,
  lastmodified timestamp NOT NULL DEFAULT now(),
  status varchar(10) NOT NULL
);

insert into jspwiki(name,text,author,status) VALUES ('test','text','me','ACTIVE');
select * from jspwiki;