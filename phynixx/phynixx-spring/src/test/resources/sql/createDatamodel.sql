-- ALTER TABLE EVAL_ROLE DROP PRIMARY KEY CASCADE;
-- DROP TABLE EVAL_ROLE CASCADE CONSTRAINTS;

--ALTER TABLE EVAL_USER DROP PRIMARY KEY CASCADE;
--DROP TABLE EVAL_USER CASCADE CONSTRAINTS;
--DROP SEQUENCE SEQ_EVAL_USER_ROLE;




CREATE SEQUENCE SEQ_ITEM
  START WITH 1
  MAXVALUE 999
  MINVALUE 1;

  
CREATE TABLE ITEM
(
  ID_ITEM               INTEGER   NOT NULL,
  VALUE_ITEM            VARCHAR(50) ,
  THREAD_ITEM           VARCHAR(50)  
);

ALTER TABLE ITEM ADD PRIMARY KEY  (ID_ITEM);

