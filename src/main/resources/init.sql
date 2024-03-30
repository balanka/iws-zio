DROP table if exists customer;
create table if not exists customer
(
    id              varchar(50)                      not null
        primary key,
    name            varchar(255)                     not null,
    description     varchar(255)                     not null,
    street          varchar(50),
    city            varchar(50),
    state           varchar(50),
    zip             varchar(50),
    phone           varchar(50),
    email           varchar(50),
    account         varchar(50),
    vatcode         varchar(50),
    oaccount varchar(50),
    postingdate    timestamp   default CURRENT_TIMESTAMP not null,
    changedate   timestamp   default CURRENT_TIMESTAMP not null,
    enterdate      timestamp   default CURRENT_TIMESTAMP not null,
    company         varchar(50)                      not null,
    modelid         integer     default 3            not null,
    country         varchar(50) default 'DE'::character varying
);
DROP table if exists supplier;
create table if not exists supplier(
                                       id             varchar(50)                                not null
                                           primary key,
                                       name           varchar(255)                               not null,
                                       description    varchar(255)                               not null,
                                       street         varchar(50),
                                       city           varchar(50),
                                       state          varchar(50),
                                       zip            varchar(50),
                                       phone          varchar(50),
                                       email          varchar(50),
                                       account        varchar(50),
                                       vatcode        varchar(50),
                                       oaccount varchar(50),
                                       postingdate   timestamp        default CURRENT_TIMESTAMP           not null,
                                       changedate  timestamp        default CURRENT_TIMESTAMP           not null,
                                       enterdate     timestamp        default CURRENT_TIMESTAMP           not null,
                                       company        varchar(50)                                not null,
                                       modelid        integer     default 1                      not null,
                                       country        varchar(50) default 'DE'::character varying not null
);
DROP table if exists employee;
create table if not exists employee(
                                       id             varchar(50)  not null  primary key,
                                       name           varchar(255) not null,
                                       description    varchar(255) not null,
                                       street         varchar(50),
                                       city           varchar(50),
                                       state          varchar(50),
                                       zip            varchar(50),
                                       phone          varchar(50),
                                       email          varchar(50),
                                       account        varchar(50),
                                       vatcode        varchar(50),
                                       oaccount varchar(50),
                                       postingdate   timestamp        default CURRENT_TIMESTAMP           not null,
                                       changedate  timestamp        default CURRENT_TIMESTAMP           not null,
                                       enterdate     timestamp        default CURRENT_TIMESTAMP           not null,
                                       company        varchar(50)                                not null,
                                       modelid        integer     default 5                      not null,
                                       country        varchar(50) default 'DE'::character varying not null,
                                       salary        numeric(12, 2) default 0
);

DROP table if exists bankaccount;
create table if not exists bankaccount
(
    id    varchar(50)  not null,
    owner   varchar(255) not null,
    bic     varchar(50)  not null,
    company varchar(50)  not null,
    modelid integer      not null,
    constraint bankaccount_unique
        unique (owner, id)
);
create unique index bankaccount_idx on bankaccount (id, owner);


DROP sequence if exists bankstatement_id_seq;
create sequence if not exists bankstatement_id_seq;

DROP table if exists bankstatement;
CREATE TABLE if not exists  bankstatement(
                                             id   bigint default nextval('bankstatement_id_seq'::regclass) not null
                                                 constraint bankstatement_pkey
                                                     primary key,
                                             depositor   varchar(50),
                                             postingdate timestamp,
                                             valuedate   timestamp,
                                             postingtext varchar(300),
                                             purpose     varchar(350),
                                             beneficiary varchar(250),
                                             accountno   varchar(50),
                                             bank_code    varchar(50),
                                             amount      numeric(12, 2),
                                             currency    varchar(200),
                                             info        varchar(250),
                                             company     varchar(50),
                                             company_iban varchar(50),
                                             posted      boolean,
                                             modelid     integer,
                                             period int4 NULL);
DROP table if exists costcenter;
create table if not exists public.costcenter
(
    id          varchar(50)                    not null
        primary key,
    name        varchar(255)                   not null,
    description varchar(255),
    account     varchar(50)                    not null,
    postingdate timestamp   default CURRENT_TIMESTAMP not null,
    changedate  timestamp   default CURRENT_TIMESTAMP not null,
    enterdate   timestamp   default CURRENT_TIMESTAMP not null,
    company     varchar(50)                    not null,
    modelid     integer   default 6            not null
);

DROP table if exists masterfile;
create table if not exists public.masterfile
(
    id          varchar(50)                    not null,
    name        varchar(255)                   not null,
    description varchar(255),
    parent     varchar(50)                    not null,
    postingdate timestamp   default CURRENT_TIMESTAMP not null,
    changedate  timestamp   default CURRENT_TIMESTAMP not null,
    enterdate   timestamp   default CURRENT_TIMESTAMP not null,
    company     varchar(50)                    not null,
    modelid     integer               not null,
    PRIMARY KEY(id, modelid, company)
);

DROP table if exists bankstatement_file;
create table if not exists public.bankstatement_file
(
    id  varchar(50) not null primary key,
    name        varchar(255)                   not null,
    description varchar(50000),
    extension varchar(50) not null,
    postingdate timestamp   default CURRENT_TIMESTAMP not null,
    changedate  timestamp   default CURRENT_TIMESTAMP not null,
    enterdate   timestamp   default CURRENT_TIMESTAMP not null,
    company     varchar(50)                    not null,
    modelid     integer   default 81            not null
);
DROP table if exists module;
create table if not exists public.module
(
    id          varchar(50)                       not null
        primary key,
    name        varchar(255)                      not null,
    description varchar(255),
    postingdate timestamp   default CURRENT_TIMESTAMP not null,
    changedate  timestamp   default CURRENT_TIMESTAMP not null,
    enterdate   timestamp   default CURRENT_TIMESTAMP not null,
    company     varchar(50)                       not null,
    modelid     integer      default 6            not null,
    path        varchar(255) default '/'::character varying,
    parent        varchar(50) default ''::character varying
);

DROP table if exists salary_item;
create table  if not exists salary_item
(
    id            varchar(50)  not null primary key,
    name          varchar(255)                   not null,
    description   varchar(255),
    account       varchar(50)  not null ,
    amount        numeric(12, 2) default 0,
    percentile  numeric(12, 2) default 0,
    postingdate  timestamp   default CURRENT_TIMESTAMP not null,
    changedate timestamp   default CURRENT_TIMESTAMP not null,
    enterdate    timestamp   default CURRENT_TIMESTAMP not null,
    company       varchar(50)                    not null,
    modelid       integer                        not null
);
DROP table if exists store;
create table   if not exists store
(
    id            varchar(50)                    not null primary key,
    name          varchar(255)                   not null,
    description   varchar(255),
    account          varchar(50)                   ,
    postingdate  timestamp   default CURRENT_TIMESTAMP not null,
    changedate timestamp   default CURRENT_TIMESTAMP not null,
    enterdate    timestamp   default CURRENT_TIMESTAMP not null,
    company       varchar(50)                    not null,
    modelid       integer                        not null
);
DROP table if exists vat;
create table  if not exists vat
(
    id               varchar(50)                         not null
        primary key,
    name             varchar(255)                        not null,
    description      varchar(255),
    percent          numeric(12, 2) default 0            not null,
    input_vat_account  varchar(50)                         not null,
    output_vat_account varchar(50)                         not null,
    postingdate     timestamp   default CURRENT_TIMESTAMP not null,
    changedate    timestamp   default CURRENT_TIMESTAMP not null,
    enterdate     timestamp   default CURRENT_TIMESTAMP not null,
    company          varchar(50)                         not null,
    modelid          integer                             not null
);
DROP table if exists account;
create table  if not exists account
(
    id            varchar(50)  not null primary key,
    name          varchar(255)                        not null,
    description   varchar(255),
    postingdate  timestamp      default CURRENT_TIMESTAMP not null,
    changedate timestamp      default CURRENT_TIMESTAMP not null,
    enterdate    timestamp      default CURRENT_TIMESTAMP not null,
    company       varchar(50)                         not null,
    modelid       integer        default 9            not null,
    account       varchar(50),
    is_debit       boolean        default true         not null,
    balancesheet  boolean        default false        not null,
    idebit        numeric(12, 2) default 0,
    icredit       numeric(12, 2) default 0,
    debit         numeric(12, 2) default 0,
    credit        numeric(12, 2) default 0,
    currency      varchar(5)     default ''::character varying
);
DROP table if exists periodic_account_balance;
create table if not exists periodic_account_balance
(
    account  varchar(50)                not null,
    name  varchar(255)                not null,
    period   integer                    not null,
    idebit   numeric(12, 2) default 0   not null,
    icredit  numeric(12, 2) default 0   not null,
    debit    numeric(12, 2) default 0   not null,
    credit   numeric(12, 2) default 0   not null,
    company  varchar(50)                not null,
    currency varchar(50)                not null,
    modelid  integer        default 106 not null,
    id       varchar(50)                not null
        constraint periodic_account_balance_pk
            primary key
);
DROP SEQUENCE IF EXISTS transaction_id_seq ;
CREATE SEQUENCE transaction_id_seq
    INCREMENT 1
    MINVALUE 1 --5633
    MAXVALUE 9223372036854775807
    START 2
    CACHE 1;
DROP table if exists transaction;
create table if not exists transaction
(
    id           bigint  default nextval('transaction_id_seq'::regclass) not null
    primary key,
    oid          bigint                            not null,
    id1          bigint                            not null,
    store   varchar(50)                       not null,
    costcenter      varchar(50)                       not null,
    text   varchar(380) default ''::character varying,
    transdate    timestamp    default CURRENT_DATE not null,
    postingdate  timestamp    default CURRENT_DATE not null,
    enterdate    timestamp    default CURRENT_DATE not null,
    company      varchar(50)                       not null,
    posted       boolean      default false,
    modelid      integer                           not null,
    period       integer
    );
DROP SEQUENCE IF EXISTS transaction_details_id_seq ;
CREATE SEQUENCE transaction_details_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
DROP table if exists transaction_details;
create table if not exists transaction_details
(
    id       bigint  default nextval('transaction_details_id_seq'::regclass) not null
    constraint transaction_details_pkey primary key,
    transid  bigint                                      not null,
    article  varchar(50) default NULL::character varying not null,
    article_name  varchar(50) default NULL::character varying not null,
    quantity   numeric(12, 2)                              not null,
    unit varchar(50) default NULL::character varying not null,
    price   numeric(12, 2)                              not null,
    currency varchar(10)                                 not null,
    duedate  timestamp   default CURRENT_DATE            not null,
    text     varchar(380)
    );

DROP SEQUENCE IF EXISTS transaction_log_id_seq ;
CREATE SEQUENCE transaction_log_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
DROP table if exists transaction_log;
create table if not exists transaction_log
(
    id           bigint  default nextval('transaction_log_id_seq'::regclass) not null primary key,
    transid      bigint                            not null,
    oid          bigint                            ,
    store   varchar(50)                       not null,
    costcenter      varchar(50)                       not null,
    article  varchar(50)  not null,
    quantity   numeric(12, 2)                              not null,
    stock   numeric(12, 2)                              not null,
    whole_stock   numeric(12, 2)                              not null,
    unit varchar(50)  not null,
    price   numeric(12, 2)                              not null,
    avg_price   numeric(12, 2)                          not null,
    currency varchar(10)                                 not null,
    duedate  timestamp               not null,
    text     varchar(380)  ,
    transdate    timestamp     not null,
    postingdate  timestamp     not null,
    enterdate    timestamp     not null,
    company      varchar(50)                       not null,
    modelid      integer                           not null,
    period       integer

);
CREATE INDEX CONCURRENTLY ON transaction_log (store, article, company);

DROP SEQUENCE IF EXISTS master_compta_id_seq ;
CREATE SEQUENCE master_compta_id_seq
    INCREMENT 1
    MINVALUE 1 --5633
    MAXVALUE 9223372036854775807
    START 2
    CACHE 1;
DROP table if exists master_compta;
create table if not exists master_compta
(
    id           bigint  default nextval('master_compta_id_seq'::regclass) not null
        primary key,
    oid          bigint                            not null,
    id1          bigint                            not null,
    costcenter   varchar(50)                       not null,
    account      varchar(50)                       not null,
    text   varchar(380) default ''::character varying,
    transdate    timestamp    default CURRENT_DATE not null,
    postingdate  timestamp    default CURRENT_DATE not null,
    enterdate    timestamp    default CURRENT_DATE not null,
    company      varchar(50)                       not null,
    file_content integer      default '-1'::integer,
    posted       boolean      default false,
    modelid      integer                           not null,
    period       integer,
    type_journal integer      default 0
);

DROP SEQUENCE IF EXISTS details_compta_id_seq ;
CREATE SEQUENCE details_compta_id_seq
    INCREMENT 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1;
--alter sequence details_compta_id_seq owner to postgres;
DROP table if exists details_compta;
create table if not exists details_compta
(
    id       bigint  default nextval('details_compta_id_seq'::regclass) not null
        constraint detailcompta_pkey
            primary key,
    transid  bigint                                      not null,
    account  varchar(50) default NULL::character varying not null,
    side     boolean                                     not null,
    oaccount varchar(50)                                 not null,
    amount   numeric(12, 2)                              not null,
    duedate  timestamp   default CURRENT_DATE            not null,
    text     varchar(380),
    currency varchar(10)                                 not null,
    terms    varchar(250),
    posted   boolean     default false,
    company  varchar(50) default 1000,
    account_name varchar(255) not null,
    oaccount_name varchar(255) not null
);
DROP table if exists financialstransaction;
create table if not exists financialstransaction
(
    id          bigint                            not null
        primary key,
    oid         bigint                            not null,
    account     varchar(50)                       not null,
    transdate   timestamp    default CURRENT_DATE not null,
    enterdate   timestamp    default CURRENT_DATE not null,
    postingdate timestamp    default CURRENT_DATE not null,
    period      integer,
    posted      boolean      default false,
    modelid     integer                           not null,
    company     varchar(50)                       not null,
    headertext  varchar(380) default ''::character varying,
    file        integer      default '-1'::integer,
    lid         bigint                            not null,
    side        boolean      default true,
    oaccount    varchar(50)                       not null,
    amount      numeric(12, 2),
    currency    varchar(50)                       not null,
    terms       varchar(380) default ''::character varying
);
DROP sequence if exists journal_id_seq;
create sequence journal_id_seq;
DROP table if exists journal;
create table if not exists journal
(
    id           bigint  default nextval('journal_id_seq'::regclass) not null
        primary key,
    transid      bigint                                              not null,
    oid          bigint,
    account      varchar(50)                                         not null,
    oaccount     varchar(50)                                         not null,
    transdate    timestamp                                           not null,
    enterdate    timestamp                                           not null,
    postingdate  timestamp                                           not null,
    period       integer                                             not null,
    amount       numeric(12, 2),
    company      varchar(50)                                         not null,
    currency     varchar(50)                                         not null,
    text         varchar(255)                                        not null,
    month        integer,
    year         integer                                             not null,
    modelid      integer                                             not null,
    --file_content integer default 0,
    --journal_type integer default 0,
    idebit       numeric(12, 2),
    debit        numeric(12, 2),
    icredit      numeric(12, 2),
    credit       numeric(12, 2),
    side         boolean
);
DROP table if exists company;
create table if not exists company
(
    id                    varchar                        not null
        primary key,
    name                  varchar                        not null,
    street                varchar,
    zip                   varchar,
    city                  varchar,
    state                 varchar,
    country               varchar,
    email                 varchar,
    partner               varchar,
    phone                 varchar,
    bank_acc               varchar                        not null,
    iban                 varchar,
    tax_code             varchar                        not null,
    vat_code               varchar                        not null,
    currency              varchar                        not null,
    locale                varchar,
    balance_sheet_acc     varchar                        not null,
    income_stmt_acc      varchar                        not null,
    modelid               integer                        not null
);
DROP table if exists users;
create table if not exists users
(
    id         integer generated always as identity
        primary key,
    user_name  varchar                                           not null
        unique,
    first_name varchar                                           not null,
    last_name  varchar                                           not null,
    email      varchar                                           not null,
    hash       varchar                                           not null,
    phone      varchar                                           not null,
    department       varchar     default 'Customer'::character varying not null,
    menu      varchar                                           not null,
    company    varchar(20) default '1000'::character varying     not null,
    modelid    integer     default 111
);
drop table  if  exists role;
create table if not exists role
(
    id     integer not null primary key,
    name   varchar(255) not null,
    description     varchar(255)  not null,
    changedate    timestamp default CURRENT_DATE not null,
    enterdate    timestamp default CURRENT_DATE not null,
    postingdate  timestamp default CURRENT_DATE not null,
    company varchar(50)  not null,
    modelid integer      default 121
);

drop table  if  exists Permission;
create table if not exists Permission
(
    id     integer not null primary key,
    name   varchar(255) not null,
    description     varchar(255)  not null,
    changedate    timestamp default CURRENT_DATE not null,
    enterdate    timestamp default CURRENT_DATE not null,
    postingdate  timestamp default CURRENT_DATE not null,
    short varchar(10)  not null,
    company varchar(50)  not null,
    modelid integer      default 141
);
drop table  if  exists user_right;
create table if not exists user_right
(
    moduleid     integer not null,
    roleid     integer not null,
    short   varchar(50) not null,
    company varchar(50)  not null,
    modelid integer      default 131,
    PRIMARY KEY(moduleid, roleid, short)
);

drop table  if  exists user_role;
create table if not exists user_role
(
    userid     integer not null,
    roleid     integer not null,
    company varchar(50)  not null,
    modelid integer      default 161,
    PRIMARY KEY(userid, roleid, company)
);
drop table  if  exists fmodule;
create table if not exists fmodule
(
    id     integer not null primary key,
    name   varchar(255) not null,
    description     varchar(255)  not null,
    changedate    timestamp default CURRENT_DATE not null,
    enterdate    timestamp default CURRENT_DATE not null,
    postingdate  timestamp default CURRENT_DATE not null,
    account varchar(50)  not null,
    is_debit boolean,
    parent varchar(50)  not null,
    company varchar(50)  not null,
    modelid integer  not null
);

drop table  if  exists asset;
create table if not exists asset
(
    id  varchar(50) not null primary key,
    name         varchar(255) not null,
    description  varchar(255),
    changedate    timestamp default CURRENT_DATE not null,
    enterdate    timestamp default CURRENT_DATE not null,
    postingdate  timestamp default CURRENT_DATE not null,
    company    varchar(50) not null,
    modelId      int not null ,
    account      varchar(50),
    oaccount     varchar(50),
    scrap_value   numeric(12, 2),
    life_span     int,
    dep_method int,
    amount       decimal(12, 2),
    rate         decimal(12, 2),
    frequency    int,
    currency     varchar(50) not null
);
insert into asset(id,  name, description, changedate, enterdate, postingdate, company, modelid, account, oaccount, scrap_value, life_span, dep_method, amount, rate, frequency, currency) values
                                                                                                                                                                                      ('1804', 'BMW-220D', 'BMW-220D', '2018-11-07 00:00:00', '2018-11-07 00:00:00', '2018-11-07 00:00:00', '1000', 19, '0520', '6222', 1.0, 5, 2, 33000.00, 0.30, 12, 'EUR'),
                                                                                                                                                                                      ('IWS-01', 'IWS', 'Integriertes Finanzbuchhaltungssystem', '2019-04-11 00:00:00','2019-04-11 00:00:00', '2019-04-11 00:00:00', '1000', 19, '0135', '6222', 1.0, 5, 2, 10000.00, 1.0, 12, 'EUR'),
                                                                                                                                                                                      ('MACB001', 'MacBook Pro 2017', 'MacBook Pro 2017', '2019-03-15 00:00:00', '2019-03-15 00:00:00','2019-03-15 00:00:00', '1000', 19, '0651', '4830', 1.0, 3, 2, 1000.00, 1.0, 12, 'EUR' ),
                                                                                                                                                                                      ('MACB002', 'MackBookPro 2019', 'MackBookPro 2019', '2019-08-09 15:53:37', '2019-08-09 15:53:37', '2019-08-09 15:53:37', '1000', 19, '0652', '4830', 1.0, 3, 2, 1000.00, 1.0, 12, 'EUR'),
                                                                                                                                                                                      ('MACB003', 'MackBookPro 2019-2', 'MackBookPro 2019-2', '2019-11-25 12:32:00', '2019-11-25 12:32:00', '2019-11-25 12:32:00', '1000', 19, '0653', '4830', 1.0, 3, 2, 1000.00, 1.0, 12, 'EUR');

drop table  if  exists article;
create table if not exists article
(
    id  varchar(50) not null primary key,
    name         varchar(255) not null,
    description  varchar(255),
    parent  varchar(50),
    sprice   numeric(12, 2),
    pprice   numeric(12, 2),
    avg_price   numeric(12, 2),
    currency  varchar(50) not null,
    stocked  boolean,
    quantity_unit  varchar(50) not null,
    pack_unit  varchar(50) not null,
    stock_account  varchar(50) not null,
    expense_account  varchar(50) not null,
    changedate    timestamp default CURRENT_DATE not null,
    enterdate    timestamp default CURRENT_DATE not null,
    postingdate  timestamp default CURRENT_DATE not null,
    company    varchar(50) not null,
    modelId      int not null);

DROP table if exists salary_item;
CREATE TABLE IF NOT EXISTS public.salary_item
(
    id character varying(50) COLLATE pg_catalog."default" NOT NULL,
    name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    description character varying(255) COLLATE pg_catalog."default",
    account character varying(50) COLLATE pg_catalog."default" NOT NULL,
    amount numeric(12,2) DEFAULT 0,
    percentile numeric(12,2) DEFAULT 0,
    postingdate timestamp without time zone NOT NULL DEFAULT CURRENT_DATE,
    changedate timestamp without time zone NOT NULL DEFAULT CURRENT_DATE,
    enterdate timestamp without time zone NOT NULL DEFAULT CURRENT_DATE,
    company character varying(50) COLLATE pg_catalog."default" NOT NULL,
    modelid integer NOT NULL DEFAULT 6,
    CONSTRAINT salary_item_pkey PRIMARY KEY (id, company)
);
DROP table if exists employee_salary_item;
CREATE TABLE IF NOT EXISTS public.employee_salary_item
(
    id character varying(50) COLLATE pg_catalog."default" NOT NULL,
    owner varchar(50) NOT NULL,
    account character varying(50) COLLATE pg_catalog."default" NOT NULL,
    amount numeric(12,2) DEFAULT 0,
    text character varying(255) NOT NULL,
    company character varying(50) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT employee_salary_item_pkey PRIMARY KEY (id, owner, account, company)
);
DROP table if exists payroll_tax_range;
CREATE TABLE IF NOT EXISTS public.payroll_tax_range
(
    id character varying(50) COLLATE pg_catalog."default" NOT NULL,
    from_amount numeric(12,2) DEFAULT 0,
    to_amount numeric(12,2) DEFAULT 0,
    tax numeric(12,2) DEFAULT 0,
    tax_class character varying(50) COLLATE pg_catalog."default",
    company character varying(50) COLLATE pg_catalog."default" NOT NULL,
    modelid integer NOT NULL DEFAULT 172,
    CONSTRAINT payroll_tax_range_pkey PRIMARY KEY (id, company)
    );
CREATE INDEX payroll_tax_range_idx ON payroll_tax_range (modelid, company);

DROP table if exists stock;
CREATE TABLE IF NOT EXISTS public.stock
(   id character varying(50) COLLATE pg_catalog."default" NOT NULL,
    store character varying(50) COLLATE pg_catalog."default" NOT NULL,
    article character varying(50) COLLATE pg_catalog."default" NOT NULL,
    quantity numeric(12,2) DEFAULT 0,
    charge character varying(50) DEFAULT '',
    company character varying(50) COLLATE pg_catalog."default" NOT NULL,
    modelid integer NOT NULL DEFAULT 37,
    CONSTRAINT stock_pkey PRIMARY KEY (id,  company)
);
CREATE INDEX stock_idx ON stock (store, article, company );

insert into article (id,  name, description, parent, sprice, pprice, avg_price,currency, stocked, quantity_unit, pack_unit, stock_account, expense_account, company, modelid) values
 ('iws001', 'Licence IWS base', 'Licence IWS base including masterfile, and administration', '-1', 1,1,1,'EUR', false, 'pc', 'pc', '5400', '5000', '1000', 35),
 ('iws002', 'Licence IWS sales', 'Licence IWS sales including 1 Y customer care', '-1', 1,1,1,'EUR', false, 'pc', 'pc', '5400', '5000', '1000', 35),
 ('iws003', 'Licence IWS purchasing', 'Licence IWS purchasing including 1 Y customer care''', '-1', 1,1,1,'EUR', false, 'pc', 'pc', '5400', '5000', '1000', 35),
 ('iws004', 'Licence IWS financials', 'Licence IWS financials including 1 Y customer care''', '-1', 1,1,1,'EUR', false, 'pc', 'pc', '5400', '5000', '1000', 35),
 ('MACS001', 'Mac Studio 2023', 'Mac Studio 2023', '-1', 1,1,1,'EUR', false, 'pc', 'pc', '5400', '5000', '1000', 35),
 ('0000', 'Atikel_0', 'Atikel_0', '-1', 0,0,0,'EUR', true, 'stk', 'stk', '5400', '5000', '1000', 35),
 ('0001', 'Atikel_1', 'Atikel_1', '-1', 0,0,0,'EUR', true, 'stk', 'stk', '5400', '5000', '1000', 35);


insert into store (id,  name, description, account, company, modelid) values
                   ('001', 'Zentral-Lager', 'Zentral-Lager', '', '1000', 35),
                   ('002', 'Nebenlager', 'Nebenlager', '','1000', 35);
insert into stock (id, store, article, quantity, charge, company, modelid) values ('31100011000', '311', '0001', 0, '', '1000', 37);

insert into fmodule (id,  name, description, account, is_debit, parent, company, modelid) values
                    (112, 'Payables', 'Payables/Supplier invoices', '1810', false, '1300', '1000', 151),
                    (114, 'Payment', 'Payment', '1810', false, '1300', '1000', 151),
                    (122, 'Receivables', 'Receivables/Customer invoices', '1810', false, '1300', '1000', 151),
                    (124, 'Settlement', 'Settlement', '1810', false, '1300', '1000', 151),
                    (134, 'General ledger', 'General ledger', '1810', false, '1300', '1000', 151),
                    (136, 'Payroll', 'Payroll', '1810', false, '1300', '1000', 151),
                    (137,'Purchase order', 'Purchase order', '-1', false,'1301', '1000', 151),
                    (138,'Goodreceiving', 'Goodreceiving', '-1', false,'1301', '1000', 151),
                    (139,'Supplier invoice', 'Supplier invoice', '-1', false,'1301', '1000', 151),
                    (109,'Sales order', 'Sales order', '-1', false,'1301', '1000', 151),
                    (110,'Bill of delivery', 'Bill of delivery', '-1', false,'1301', '1000', 151),
                    (111,'Customer invoice', 'Customer invoice', '-1', false,'1301', '1000', 151);

insert into role (id,  name, description, company, modelid) values
                 (-1, 'devops', 'DevOps', '1000', 121),
                 (1, 'admin', 'Administrator', '1000', 121),
                 (2, 'dev', 'Developer', '1000', 121),
                 (3, 'acc', 'Accountant', '1000', 121),
                 (4, 'acc_senior', 'Senior Accountant', '1000', 121),
                 (5, 'acc_assist', 'Accountant assistant', '1000', 121),
                 (6, 'logistic', 'Logistic', '1000', 121),
                 (7, 'logistic_senior', 'Senior Logistic', '1000', 121),
                 (8, 'logistic_assist', 'Logistic assistant', '1000', 121);

insert into user_role (userid,  roleid,  company, modelid) values
                                                               (1, -1,  1000, 161),
                                                               (1, 1,  1000, 161),
                                                               (1, 2,  1000, 161),
                                                               (2, 1,  1000, 161),
                                                               (2, 2,  1000, 161),
                                                               (4, 1,  1000, 161),
                                                               (4, 2,  1000, 161);


insert into Permission (id,  name, description, short, company, modelid) values
                                                                             (1, 'CREATE', 'create an instance module', '+', '1000', 141),
                                                                             (2, 'READ', 'Access, Read and display a module', 'r', '1000',141),
                                                                             (3, 'WRITE', 'Access, Read and display and modify a module', 'w', '1000', 141),
                                                                             (4, 'DELETE', 'Access, Read and display, modify and delete a module', '-', '1000', 141),
                                                                             (5, 'POST', 'Access, Read and display and post a transaction', 'p', '1000', 141),
                                                                             (6, 'REPORT', 'Access, Read and display and print a report', 't', '1000', 141);

insert into user_right (moduleid,  roleid, short, company, modelid) values
                                                                        (1, 1, '+', 1000, 131),
                                                                        (1, 1, 'r', 1000, 131),
                                                                        (1, 1, 'w', 1000, 131),
                                                                        (3, 1, '+', 1000, 131),
                                                                        (3, 1, 'r', 1000, 131),
                                                                        (3, 1, 'w', 1000, 131),
                                                                        (6, 1, '+', 1000, 131),
                                                                        (6, 1, 'r', 1000, 131),
                                                                        (6, 1, 'w', 1000, 131),
                                                                        (9, 1, '+', 1000, 131),
                                                                        (9, 1, 'r', 1000, 131),
                                                                        (9, 1, 'w', 1000, 131),
                                                                        (10, 1, '+', 1000, 131),
                                                                        (10, 1, 'r', 1000, 131),
                                                                        (10, 1, 'w', 1000, 131),
                                                                        (11, 1, '+', 1000, 131),
                                                                        (11, 1, 'r', 1000, 131),
                                                                        (11, 1, 'w', 1000, 131),
                                                                        (14, 1, '+', 1000, 131),
                                                                        (14, 1, 'r', 1000, 131),
                                                                        (14, 1, 'w', 1000, 131),
                                                                        (18, 1, '+', 1000, 131),
                                                                        (18, 1, 'r', 1000, 131),
                                                                        (18, 1, 'w', 1000, 131),
                                                                        (20, 1, 'r', 1000, 131),
                                                                        (30, 1, 'r', 1000, 131),
                                                                        (106, 1, 'r', 1000, 131),
                                                                        (106, 1, 'w', 1000, 131),
                                                                        (106, 1, 't', 1000, 131),
                                                                        (111, 1, '+', 1000, 131),
                                                                        (111, 1, 'r', 1000, 131),
                                                                        (111, 1, 'w', 1000, 131),
                                                                        (400, 1, 'r', 1000, 131),
                                                                        (1010, 1, 'r', 1000, 131),
                                                                        (1300, 1, 'r', 1000, 131),
                                                                        (10002, 1, 'r', 1000, 131),
                                                                        (10012, 1, 'r', 1000, 131),
                                                                        (11111, 1, 'r', 1000, 131),
                                                                        (1, 2, '+', 1000, 131),
                                                                        (1, 2, 'r', 1000, 131),
                                                                        (1, 2, 'w', 1000, 131),
                                                                        (3, 2, '+', 1000, 131),
                                                                        (3, 2, 'r', 1000, 131),
                                                                        (3, 2, 'w', 1000, 131),
                                                                        (6, 2, '+', 1000, 131),
                                                                        (6, 2, 'r', 1000, 131),
                                                                        (6, 2, 'w', 1000, 131),
                                                                        (9, 2, '+', 1000, 131),
                                                                        (9, 2, 'r', 1000, 131),
                                                                        (9, 2, 'w', 1000, 131),
                                                                        (10, 2, '+', 1000, 131),
                                                                        (10, 2, 'r', 1000, 131),
                                                                        (10, 2, 'w', 1000, 131),
                                                                        (11, 2, '+', 1000, 131),
                                                                        (11, 2, 'r', 1000, 131),
                                                                        (11, 2, 'w', 1000, 131),
                                                                        (14, 2, '+', 1000, 131),
                                                                        (14, 2, 'r', 1000, 131),
                                                                        (14, 2, 'w', 1000, 131),
                                                                        (18, 2, '+', 1000, 131),
                                                                        (18, 2, 'r', 1000, 131),
                                                                        (18, 2, 'w', 1000, 131),
                                                                        (20, 2, 'r', 1000, 131),
                                                                        (30, 2, 'r', 1000, 131),
                                                                        (106, 2, 'r', 1000, 131),
                                                                        (106, 2, 'w', 1000, 131),
                                                                        (106, 2, 't', 1000, 131),
                                                                        (111, 2, '+', 1000, 131),
                                                                        (111, 2, 'r', 1000, 131),
                                                                        (111, 2, 'w', 1000, 131),
                                                                        (400, 2, 'r', 1000, 131),
                                                                        (1010, 2, 'r', 1000, 131),
                                                                        (1300, 2, 'r', 1000, 131),
                                                                        (10002, 2, 'r', 1000, 131),
                                                                        (10012, 2, 'r', 1000, 131),
                                                                        (11111, 2, 'r', 1000, 131);




insert into company (id, name, street, zip, city, state, country, email, partner, phone, bank_acc, iban, tax_code, vat_code, currency, locale, balance_sheet_acc, income_stmt_acc, modelid)
values ('1000', 'ABC GmbH', 'Word stree1 0','49110','FF', 'DE','Deutschland', 'info@mail.com','John', '+001-00000'
       ,'1810','DE', 'XXX/XXXX/XXXX','v5','EUR',  'de_DE', '9900', '9800', 10);


insert into account
(id, name, description, enterdate, postingdate, changedate, company, modelid, account,is_debit, balancesheet, currency, idebit, icredit, debit, credit ) values
                                                                                                                                                             ('9900','Bilanz','Bilanz',current_timestamp, current_timestamp, current_timestamp, '1000',9
                                                                                                                                                             , '', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('9901','Bilanz Aktiva','Bilanz Aktiva',current_timestamp, current_timestamp, current_timestamp, '1000',9
                                                                                                                                                             , '9900', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('9902','Bilanz Passiva','Bilanz Passiva',current_timestamp, current_timestamp, current_timestamp, '1000',9
                                                                                                                                                             , '9900', false, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('9800','GuV Aktiva','GuV Aktiva',current_timestamp, current_timestamp, current_timestamp, '1000',9
                                                                                                                                                             , '9902', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('9801','GuV Aktiva','GuV Aktiva',current_timestamp, current_timestamp, current_timestamp, '1000',9
                                                                                                                                                             , '9800', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('9802','GuV Passiva','GuV Passiva',current_timestamp, current_timestamp, current_timestamp, '1000',9
                                                                                                                                                             , '9800', false, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('4000','Umsatzerloese','Umsatzerloese',current_timestamp, current_timestamp, current_timestamp, '1000',9
                                                                                                                                                             , '9802', false, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('4400','Umsatzerloese 19%','Umsatzerloese 19%',current_timestamp, current_timestamp, current_timestamp, '1000',9
                                                                                                                                                             , '4000', false, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('1200','Forderungen aus Lieferungen und Leistungen','Forderung a. L & L',current_timestamp, current_timestamp, current_timestamp, '1000',9
                                                                                                                                                             , '9901', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('1217','Forderung1','Forderung1',current_timestamp, current_timestamp, current_timestamp, '1000',9, '9901', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('1800','Bank','Bank',current_timestamp, current_timestamp, current_timestamp, '1000',9
                                                                                                                                                             , '9901', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('1810','Giro SPK Bielefeld','Giro SPK Bielefeld',current_timestamp, current_timestamp, current_timestamp, '1000',9, '1800', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('1600','Kasse','Kasse',current_timestamp, current_timestamp, current_timestamp, '1000',9, '9901', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('1601','Kasse','Kasse',current_timestamp, current_timestamp, current_timestamp, '1000',9, '1600', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('331031','Verbbindlichkeiten 1','Verbbindlichkeiten 1',current_timestamp, current_timestamp, current_timestamp, '1000',9, '331030', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('3806','MWst 19%','MWst 19%',current_timestamp, current_timestamp, current_timestamp, '1000',9, '6', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('5000','Warenbestand','Warenbestand',current_timestamp, current_timestamp, current_timestamp, '1000',9, '9901', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('5400','Wareneinsatz 19%','Wareneinsatz 19%',current_timestamp, current_timestamp, current_timestamp, '1000',9, '6', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                                                                                                             ('00000','Dummy','Dummy','2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z', '1000',9, '5', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0);


insert into periodic_account_balance
(id, account, name, period, idebit,debit,icredit,credit, company,currency,modelid)
values
    (CONCAT(to_char( CURRENT_DATE- INTERVAL '1 year', 'YYYYMM'),'1200'), '1200',  'Forderungen aus Lieferungen und Leistungen', TO_NUMBER(to_char( CURRENT_DATE- INTERVAL '1 year', 'YYYYMM'),'99999999'),
     0, 1000, 0, 0,'1000' , 'EUR', 106),
    (CONCAT(to_char( CURRENT_DATE- INTERVAL '1 year', 'YYYYMM'),'1601'), '1601', 'Kasse', TO_NUMBER(to_char( CURRENT_DATE- INTERVAL '1 year', 'YYYYMM'),'99999999'),
     0, 1000, 0, 0,'1000' , 'EUR', 106),
    (CONCAT(to_char( CURRENT_DATE, 'YYYY'),'001200'), '1200', 'Forderungen aus Lieferungen und Leistungen', TO_NUMBER(CONCAT(to_char( CURRENT_DATE, 'YYYY'),'00'),'99999999'),
     500, 0, 0, 0,'1000' , 'EUR', 106),
    (CONCAT(to_char( CURRENT_DATE, 'YYYYMM'),'1200'), '1200', 'Forderungen aus Lieferungen und Leistungen', TO_NUMBER(to_char( CURRENT_DATE, 'YYYYMM'),'99999999'),
     10, 100, 0, 0,'1000' , 'EUR', 106),
    (CONCAT(to_char( CURRENT_DATE, 'YYYYMM'),'1810'), '1810', 'Giro SPK Bielefeld', TO_NUMBER(to_char( CURRENT_DATE, 'YYYYMM'),'99999999'),
     0, 50, 0, 0,'1000' , 'EUR', 106),
    (CONCAT(to_char( CURRENT_DATE, 'YYYYMM'),'1601'), '1601', 'Kasse', TO_NUMBER(to_char( CURRENT_DATE, 'YYYYMM'),'99999999'),
     0, 200, 0, 0,'1000' , 'EUR', 106),
    (CONCAT(to_char( CURRENT_DATE, 'YYYYMM'),'4400'), '4400', 'Umsatzerloese 19%', TO_NUMBER(to_char( CURRENT_DATE, 'YYYYMM'),'99999999'),
     0, 200, 0, 0,'1000' , 'EUR', 106)   ;

insert into customer (id, name, description,street,zip,city,state, country, phone,email,account,oaccount,vatcode,company,modelid,enterdate,changedate,postingdate)
values ('5004','Kunde ( Sonstige Erloes)','Kunde ( Sonstige Erloes)','sonstige Str 1', '47111','Nirvana', 'WORLD', 'DE'
       , '+000000000', 'myMail@mail.com','1217', '1217', 'v0', '1000', 3, current_timestamp, current_timestamp, current_timestamp),
       ('5014','KKM AG', 'KKM AG','Laatzer str 0', '5009', 'Hannover', 'Niedersachsen', 'DE'
       , '+000000001', 'yourMail@mail.com','1445', '4487', 'v0', '1000', 3, current_timestamp, current_timestamp, current_timestamp),
       ('5222','Dummy', 'Dummy','Dummy', 'Dummy', 'Dummy', 'Dummy', 'DE'
       , 'Dummy', 'dummy@dummy.com','1215', '111111', 'v5', '1000', 3, '2018-01-01T10:00:00.00Z'
       , '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z');


insert into supplier (id, name, description,street,zip,city,state, country, phone,email,account,oaccount,vatcode,company,modelid,enterdate,changedate,postingdate)
values ('70000','Dummy','Dummy','', '', '', '', 'DE', '', '', '331040', '6825','v5',
        '1000', 1, '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z'),
       ('70034','Sonstige GWG Lieferenten','Sonstige GWG Lieferenten','sonstige Str 1', '47111', 'Nirvana', 'WORLD','DE'
       , '+000000000', 'myMail@mail.com','331031', '4855','v5', '1000', 1, current_timestamp, current_timestamp
       , current_timestamp),
       ('70060', 'Sonstige ITK Lieferanten', 'Sonstige ITK Lieferanten','sonstige Str 1', '47111', 'Nirvana', 'WORLD','DE'
       , '+000000000', 'myMail@mail.com','331036', '6810',  'v5',  '1000', 1, current_timestamp, current_timestamp
       , current_timestamp),
       ('70063', 'Sonstige Benzin Lieferant', 'Sonstige Benzin Lieferant','sonstige Str 1', '47111', 'Nirvana', 'WORLD','DE'
       , '+000000000', 'myMail@mail.com','331030', '6530'
       , 'v5','1000',1 , current_timestamp, current_timestamp, current_timestamp),
       ('70064','Sonstige KFZ Lieferant', 'Sonstige KFZ Lieferant','sonstige Str 1', '47111', 'Nirvana', 'WORLD','DE'
       , '+000000000', 'myMail@mail.com','331030', '6530',  'v5', '1000', 1, current_timestamp, current_timestamp
       , current_timestamp),
       ('70005','Sonstige KFZ Lieferant', 'Sonstige KFZ Lieferant','sonstige Str 1', '47111', 'Nirvana', 'WORLD','DE'
       , '+000000000', 'myMail@mail.com','331030', '6530',  'v5', '1000', 1, current_timestamp, current_timestamp
       , current_timestamp);

insert into bankaccount (id, owner, bic, company, modelid)
values ('DE27662900000001470004X','5004','SPBIDE3BXXX','1000',12),
       ('DE27662900000001470014X','5014','SPBIDE3BXXX','1000',12),
       ('DE22480501610043000000','4712','SPBIDE3BXXX','1000',12),
       ('DE27662900000001470034X', '70034','SPBIDE3BXXX','1000',12),
       ('DE22480501610043000000','70034','SPBIDE3BXXX','1000',12),
       ('DE08370501980020902219Y','70060','SPBIDE3BXXX','1000',12),
       ('DE16300500000001609163Y','70063','SPBIDE3BXXX','1000',12),
       ('DE84480501610047008271', '70005','SPBIDE3BXXX','1000',12),
       ('DE6248040035053249000Y','70064','SPBIDE3BXXX','1000',12);

insert into bankstatement
(depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bank_code,amount, currency, info, company, company_iban, posted, modelid)
values
    ('B Mady',current_timestamp, current_timestamp,'TEST POSTING','TEST PURPOSE','B Mady','DE27662900000001470034X','43007711BIC', -1000, 'EUR','INFO TXT','1000','47114300IBAN',false,18 ),
    ('KABA Soft GmbH',current_timestamp, current_timestamp,'TEST POSTING','TEST PURPOSE','KABA Soft GmbH','DE27662900000001470004X','470434300IBAN', 1000, 'EUR','INFO TXT','1000','47114300IBAN',false,18 );

insert into salary_item
(id, name, description, account, amount, percentile, postingdate, changedate, enterdate,  company, modelid)
values('4711','Lohnsteuer','Lohnsteuer', '6024', 200, 2.0, current_timestamp, current_timestamp, current_timestamp, '1000',171);
insert into masterfile
(id, name, description, parent, postingdate, changedate, enterdate,  company, modelid)
values('4711','myFirstBank','myFirstBank', '', current_timestamp, current_timestamp, current_timestamp, '1000',11),
      ('COLSDE33','SPARKASSE KOELN-BONN','SPARKASSE KOELN-BONN', '', '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z', '1000',11);

insert into masterfile (id, name, description, parent, enterdate,changedate,postingdate, modelid, company)
values('300','Production','Production','800' ,'2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z',6,'1000' ),
      ('000','Dummy','Dummy','Dummy' ,'2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z', 6,'1000' );
insert into masterfile (id, name, description, parent, enterdate,changedate,postingdate, modelid, company)
values('stk','Stueck','Stueck','' ,current_timestamp, current_timestamp, current_timestamp, 15,'1000' ),
      ('ltr','Liter','Liter','' ,current_timestamp, current_timestamp, current_timestamp, 15,'1000' ),
      ('kg','Kilogramm','Kilogramm','' ,current_timestamp, current_timestamp, current_timestamp, 15,'1000' );
-- ALTER TABLE masterfile ADD UNIQUE (modelid, company);
-- ALTER TABLE module ADD UNIQUE (modelid, company);
-- ALTER TABLE vat ADD UNIQUE (modelid, company);
-- ALTER TABLE account ADD UNIQUE (modelid, company);
-- ALTER TABLE customer ADD UNIQUE (modelid, company);
-- ALTER TABLE supplier ADD UNIQUE (modelid, company);
-- ALTER TABLE employee ADD UNIQUE (modelid, company);
-- ALTER TABLE users ADD UNIQUE (modelid, company);
-- ALTER TABLE bankaccount ADD UNIQUE (modelid, company);
-- ALTER TABLE salary_item ADD UNIQUE (modelid, company);
-- ALTER TABLE Permission ADD UNIQUE (modelid, company);
-- ALTER TABLE role ADD UNIQUE (modelid, company);
-- ALTER TABLE user_role ADD UNIQUE (modelid, company);
-- ALTER TABLE user_right ADD UNIQUE (modelid, company);
-- ALTER TABLE fmofule ADD UNIQUE (modelid, company);
-- ALTER TABLE store ADD UNIQUE (modelid, company);
-- ALTER TABLE article ADD UNIQUE (modelid, company);
-- ALTER TABLE asset ADD UNIQUE (modelid, company);
-- ALTER TABLE journal ADD UNIQUE (modelid, company);
-- ALTER TABLE periodic_account_balance ADD UNIQUE (modelid, company);
-- ALTER TABLE master_compta ADD UNIQUE (modelid, company);

insert into module (id, name, description,path, parent, enterdate,changedate,postingdate, modelid, company)
values('0000','Dummy','Dummy', '', '', '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z',300,'1000' );
--INSERT INTO public."user_right" select 33, roleid, short, company, modelid from user_right where moduleid=3;

insert into vat
(id, name, description, percent, input_vat_account, output_vat_account, postingdate, changedate, enterdate,  company, modelid)
values
    ('v101','Dummy','Dummy',0.07, '0650', '0651', '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z', '2018-01-01T10:00:00.00Z', '1000',14),
    ('4711','myFirstVat','myFirstVat',1, '1406', '3806', current_timestamp, current_timestamp, current_timestamp, '1000',14);

INSERT INTO public."module"
(id, name, description, postingdate, changedate, enterdate, company, modelid, path, parent)
VALUES('36', 'menu.AccountClass', './MasterfileForm', CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, '1000', 400, '/class', 20);
--insert into user_right select 36 as moduleid, roleid, short, company, modelid from user_right where moduleid='11';
INSERT INTO public."module"
(id, name, description, postingdate, changedate, enterdate, company, modelid, path, parent)
VALUES('37', 'menu.AccountGroup', './MasterfileForm', CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, '1000', 400, '/group', 20);
--insert into user_right select 37 as moduleid, roleid, short, company, modelid from user_right where moduleid='11';

INSERT INTO public."module"
(id, name, description, postingdate, changedate, enterdate, company, modelid, path, parent)
VALUES('15', 'menu.quantityUnit', './MasterfileForm', CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, '1000', 400, '/qty', 20);
--insert into user_right select 15 as moduleid, roleid, short, company, modelid from user_right where moduleid='11';

INSERT INTO public."module"
(id, name, description, postingdate, changedate, enterdate, company, modelid, path, parent)
VALUES('38', 'menu.closeAccountingPeriod', './MasterfileForm', CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, '1000', 400, '/close', 30);
insert into user_right select 38 as moduleid, roleid, short, company, modelid from user_right where moduleid='11';

INSERT INTO public."module"
(id, name, description, postingdate, changedate, enterdate, company, modelid, path, parent)
VALUES('39', 'menu.createPayrollTransaction', './MasterfileForm', CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, '1000', 400, '/ptr', 30);
insert into user_right select 39 as moduleid, roleid, short, company, modelid from user_right where moduleid='11';

INSERT INTO public."module"
(id, name, description, postingdate, changedate, enterdate, company, modelid, path, parent)
VALUES('41', 'menu.createDepreciationTransaction', './MasterfileForm', CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, '1000', 400, '/ftr', 30);
insert into user_right select 41 as moduleid, roleid, short, company, modelid from user_right where moduleid='11';

INSERT INTO public."module"
(id, name, description, postingdate, changedate, enterdate, company, modelid, path, parent)
VALUES('1301', 'menu.transaction', './TransactionMainForm', CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, '1000', 400, '/ltr', 30);
insert into user_right select 1301 as moduleid, roleid, short, company, modelid from user_right where moduleid='11';

INSERT INTO public."module"
(id, name, description, postingdate, changedate, enterdate, company, modelid, path, parent)
VALUES('172', 'menu.payrollTaxRange', './MasterfileForm', CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, '1000', 400, '/payrollTax', 20);
--insert into user_right select 172 as moduleid, roleid, short, company, modelid from user_right where moduleid='11';
--ALTER TABLE payroll_tax_range RENAME COLUMN taxClass TO tax_class;
insert into payroll_tax_range(id, from_amount, to_amount, tax_class, tax, company, modelid)
values('1', 4968.00, 5003.99, 'I', 0, '1000', 172),('2', 4968.00, 5003.99, 'II', 0, '1000', 172),
    ('3', 4968.00, 5003.99, 'III', 0, '1000', 172),('4', 4968.00, 5003.99, 'IV', 0, '1000', 172),
    ('5', 4968.00, 5003.99, 'V', 373, '1000', 172),('6', 4968.00, 5003.99, 'VI', 551, '1000', 172);

insert into users(user_name, first_name, last_name,  hash, email, phone, department, menu, company, modelid)
values('jdegoes011','John','dgoes', '$21a$10$0IZtq3wGiRQSMIuoIgNKrePjQfmGFRgkpnHwyY9RcbUhZxU9Ha1mCX', 'whatYourGrandma1Name@gmail.com'
      , '11-4711-0123','Admin', '1300,9,1120,3,14,1000,18,1,112,106,11,6,10', '1000',111),
      ('myUserName','myUserFirstName','myUserLastName', 'hash1', 'myEmail@email.com', '+49-1111-11100','Accountant', '1,10', '1000',111);
--insert into user_right select 172 as moduleid, roleid, short, company, modelid from user_right where moduleid='11';


INSERT INTO master_compta (id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content)
VALUES (1, -1, 1, '300', '1600', '2023-04-08T14:46:44.173Z', '2023-04-08T15:07:28.685Z', '2023-04-08T15:07:28.685Z', 202304, false, 124, '1000', '', 0, 0);
INSERT INTO details_compta (transid,  account, side, oaccount, amount, duedate, text, currency, account_name, oaccount_name)
VALUES (1,  '1200', true, '4400', 81.00, '2023-04-09T15:50:17.598252Z', 'terms', 'EUR', 'Forderungen aus Lieferungen und Leistungen', 'Umsatzerloese 19%' ),
       (1,  '1200', true, '3806', 19.00, '2023-04-09T15:50:17.598270Z', 'terms', 'EUR','Forderungen aus Lieferungen und Leistungen', 'MWSt 19%' );


-- insert  into transaction (oid, id1, store, costcenter, text, company, modelid, period) values(-1, -1, '001', '200', 'Test', '1000', 101, 202403);
-- insert  into transaction (oid, id1, store, costcenter, text, company, modelid, period) values(-1, -1, '001', '200', 'Test', '1000', 105, 202403);
-- insert  into transaction (oid, id1, store, costcenter, text, company, modelid, period) values(-1, -1, '001', '200', 'Test', '1000', 139, 202403);