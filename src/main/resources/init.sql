
create table if not exists customer
(
    id              varchar(50)                      not null
    primary key,
    name            varchar(255)                     not null,
    description     varchar(255)                     not null,
    street          varchar(255),
    city            varchar(255),
    state           varchar(255),
    zip             varchar(255),
    phone           varchar(50),
    email           varchar(50),
    account         varchar(50),
    iban            varchar(50),
    vatcode         varchar(50),
    oaccount varchar(50),
    postingdate    timestamp   default CURRENT_TIMESTAMP not null,
    changedate   timestamp   default CURRENT_TIMESTAMP not null,
    enterdate      timestamp   default CURRENT_TIMESTAMP not null,
    company         varchar(50)                      not null,
    modelid         integer     default 3            not null,
    country         varchar(50) default ''::character varying
    );

create table if not exists supplier(
    id             varchar(50)                                not null
    primary key,
    name           varchar(255)                               not null,
    description    varchar(255)                               not null,
    street         varchar(255),
    city           varchar(255),
    state          varchar(255),
    zip            varchar(255),
    phone          varchar(50),
    email          varchar(50),
    account        varchar(50),
    iban           varchar(50),
    vatcode        varchar(50),
    oaccount varchar(50),
    postingdate   timestamp        default CURRENT_TIMESTAMP           not null,
    changedate  timestamp        default CURRENT_TIMESTAMP           not null,
    enterdate     timestamp        default CURRENT_TIMESTAMP           not null,
    company        varchar(50)                                not null,
    modelid        integer     default 5                      not null,
    country        varchar(50) default 'X'::character varying not null
    );

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


create sequence bankstatement_id_seq;

CREATE TABLE bankstatement(
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
                              modelid     integer);

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
    path        varchar(255) default '/'::character varying
    );

create table  bank
(
    id            varchar(50)                    not null
        primary key,
    name          varchar(255)                   not null,
    description   varchar(255),
    postingdate  timestamp   default CURRENT_TIMESTAMP not null,
    changedate timestamp   default CURRENT_TIMESTAMP not null,
    enterdate    timestamp   default CURRENT_TIMESTAMP not null,
    company       varchar(50)                    not null,
    modelid       integer                        not null
);
create table  vat
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
create table  account
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
create table if not exists periodic_account_balance
(
    account  varchar(50)                not null,
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
DROP SEQUENCE IF EXISTS master_compta_id_seq ;
CREATE SEQUENCE master_compta_id_seq
    INCREMENT 1
   MINVALUE 1
   MAXVALUE 9223372036854775807
   START 1
   CACHE 1;

create table if not exists master_compta
(
    id           bigint  default nextval('master_compta_id_seq'::regclass) not null
    primary key,
    oid          bigint                            not null,
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
    company  varchar(50) default 1000
    );
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
create sequence journal_id_seq;
create table if not exists journal
(
    id           bigint  default nextval('journal_id_seq'::regclass) not null
    primary key,
    transid      bigint                                              not null,
    --oid          bigint,
    account      varchar(50)                                         not null,
    oaccount     varchar(50)                                         not null,
    transdate    timestamp                                           not null,
    --enterdate    timestamp                                           not null,
    --postingdate  timestamp                                           not null,
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

create table if not exists company
(
    id                    varchar                        not null
    primary key,
    name                  varchar                        not null,
    street                varchar,
    zip                   varchar,
    city                  varchar,
    state                 varchar,
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
insert into company (id, name, street, zip, city, state, email, partner, phone, bank_acc, iban, tax_code, vat_code, currency, locale, balance_sheet_acc, income_stmt_acc, modelid)
values ('1000', 'ABC GmbH', 'Word stree1 0','49110','FF', 'DE', 'info@mail.com','John', '+001-00000'
       ,'1810','DE', 'XXX/XXXX/XXXX','v5','EUR',  'de_DE', '9900', '9800', 10);


insert into account
(id, name, description, enterdate, postingdate, changedate, company, modelid, account,is_debit, balancesheet, currency, idebit, icredit, debit, credit ) values
                                                                      ('9900','Bilanz','Bilanz',current_timestamp, current_timestamp, current_timestamp, '1000',11
                                                                      , '', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('9901','Bilanz Aktiva','Bilanz Aktiva',current_timestamp, current_timestamp, current_timestamp, '1000',11
                                                                      , '9900', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('9902','Bilanz Passiva','Bilanz Passiva',current_timestamp, current_timestamp, current_timestamp, '1000',11
                                                                      , '9900', false, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('9800','GuV Aktiva','GuV Aktiva',current_timestamp, current_timestamp, current_timestamp, '1000',11
                                                                      , '9902', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('9801','GuV Aktiva','GuV Aktiva',current_timestamp, current_timestamp, current_timestamp, '1000',11
                                                                      , '9800', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('9802','GuV Passiva','GuV Passiva',current_timestamp, current_timestamp, current_timestamp, '1000',11
                                                                      , '9800', false, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('4000','Umsatzerloese','Umsatzerloese',current_timestamp, current_timestamp, current_timestamp, '1000',11
                                                                      , '9802', false, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('4400','Umsatzerloese 19%','Umsatzerloese 19%',current_timestamp, current_timestamp, current_timestamp, '1000',11
                                                                      , '4000', false, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('1200','Forderungen aus Lieferungen und Leistungen','Forderung a. L & L',current_timestamp, current_timestamp, current_timestamp, '1000',11
                                                                      , '9901', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('1800','Bank','Bank',current_timestamp, current_timestamp, current_timestamp, '1000',11
                                                                      , '9901', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('1810','Giro SPK Bielefeld','Giro SPK Bielefeld',current_timestamp, current_timestamp, current_timestamp, '1000',11, '1800', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('1600','Kasse','Kasse',current_timestamp, current_timestamp, current_timestamp, '1000',11, '9901', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0),
                                                                      ('1601','Kasse','Kasse',current_timestamp, current_timestamp, current_timestamp, '1000',11, '1600', true, true, 'EUR', 0.0, 0.0, 0.0, 0.0);

insert into periodic_account_balance
(id, account, period, idebit,debit,icredit,credit, company,currency,modelid)
values
    (CONCAT(to_char( CURRENT_DATE- INTERVAL '1 year', 'YYYYMM'),'1200'), '1200', TO_NUMBER(to_char( CURRENT_DATE- INTERVAL '1 year', 'YYYYMM'),'99999999'),
     0, 1000, 0, 0,'1000' , 'EUR', 106),
    (CONCAT(to_char( CURRENT_DATE, 'YYYY'),'001200'), '1200', TO_NUMBER(CONCAT(to_char( CURRENT_DATE, 'YYYY'),'00'),'99999999'),
     500, 0, 0, 0,'1000' , 'EUR', 106),
    (CONCAT(to_char( CURRENT_DATE, 'YYYYMM'),'1200'), '1200', TO_NUMBER(to_char( CURRENT_DATE, 'YYYYMM'),'99999999'),
     10, 100, 0, 0,'1000' , 'EUR', 106),
    (CONCAT(to_char( CURRENT_DATE, 'YYYYMM'),'1810'), '1810', TO_NUMBER(to_char( CURRENT_DATE, 'YYYYMM'),'99999999'),
     0, 50, 0, 0,'1000' , 'EUR', 106),
    (CONCAT(to_char( CURRENT_DATE, 'YYYYMM'),'1601'), '1601', TO_NUMBER(to_char( CURRENT_DATE, 'YYYYMM'),'99999999'),
    0, 200, 0, 0,'1000' , 'EUR', 106),
    (CONCAT(to_char( CURRENT_DATE, 'YYYYMM'),'4400'), '4400', TO_NUMBER(to_char( CURRENT_DATE, 'YYYYMM'),'99999999'),
     0, 200, 0, 0,'1000' , 'EUR', 106)   ;

insert into customer (id, name, description,street,zip,city,state,phone,email,account,oaccount,iban,vatcode,company,modelid,enterdate,changedate,postingdate)
values ('5004','Kunde ( Sonstige Erloes)','Kunde ( Sonstige Erloes)','sonstige Str 1', '47111','Nirvana', 'WORLD'
       , '+000000000', 'myMail@mail.com','1217', '1217', 'DE27662900000001470004X','v0', '1000', 3, current_timestamp, current_timestamp, current_timestamp),
       ('5014','KKM AG', 'KKM AG','Laatzer str 0', '5009', 'Hannover', 'Niedersachsen'
       , '+000000001', 'yourMail@mail.com','1445', '4487', 'DE27662900000001470004X','v0', '1000', 3, current_timestamp, current_timestamp, current_timestamp),
       ('5222','Dummy', 'Dummy','Dummy', 'Dummy', 'Dummy', 'Dummy'
       , 'Dummy', 'dummy@dummy.com','1215', '111111', 'DEddddddddddddddddommy','v5', '1000', 3, '2018-01-01T00:00:00.00Z'
       , '2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z');


insert into supplier (id, name, description,street,zip,city,state,phone,email,account,oaccount,iban,vatcode,company,modelid,enterdate,changedate,postingdate)
values ('70000','Dummy','Dummy','', '', '', '', '', '','331040', '6825', 'DE8448050161004700827X','v5',
        '1000', 1, '2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z'),
       ('70034','Sonstige GWG Lieferenten','Sonstige GWG Lieferenten','sonstige Str 1', '47111', 'Nirvana', 'WORLD'
       , '+000000000', 'myMail@mail.com','331031', '4855', 'DE27662900000001470034X','v5', '1000', 1, current_timestamp, current_timestamp
       , current_timestamp),
       ('70060', 'Sonstige ITK Lieferanten', 'Sonstige ITK Lieferanten','sonstige Str 1', '47111', 'Nirvana', 'WORLD'
       , '+000000000', 'myMail@mail.com','331036', '6810', 'DE08370501980020902219', 'v5',  '1000', 1, current_timestamp, current_timestamp
       , current_timestamp),
       ('70063', 'Sonstige Benzin Lieferant', 'Sonstige Benzin Lieferant','sonstige Str 1', '47111', 'Nirvana', 'WORLD'
       , '+000000000', 'myMail@mail.com','331030', '6530'
       ,'DE16300500000001609114', 'v5','1000',1 , current_timestamp, current_timestamp, current_timestamp),
       ('70064','Sonstige KFZ Lieferant', 'Sonstige KFZ Lieferant','sonstige Str 1', '47111', 'Nirvana', 'WORLD'
       , '+000000000', 'myMail@mail.com','331030', '6530', 'DE6248040035053249000Y', 'v5', '1000', 1, current_timestamp, current_timestamp
       , current_timestamp),
       ('70005','Sonstige KFZ Lieferant', 'Sonstige KFZ Lieferant','sonstige Str 1', '47111', 'Nirvana', 'WORLD'
       , '+000000000', 'myMail@mail.com','331030', '6530', 'DE84480501610047008271', 'v5', '1000', 1, current_timestamp, current_timestamp
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


insert into bank
(id, name, description, postingdate, changedate, enterdate,  company, modelid)
values('4711','myFirstBank','myFirstBank',current_timestamp, current_timestamp, current_timestamp, '1000',11),
      ('COLSDE33','SPARKASSE KOELN-BONN','SPARKASSE KOELN-BONN','2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z', '1000',11);

insert into costcenter (id, name, description,account, enterdate,changedate,postingdate, modelid, company)
values('300','Production','Production','800' ,'2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z',6,'1000' ),
      ('000','Dummy','Dummy','Dummy' ,'2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z', 6,'1000' );


insert into module (id, name, description,path,enterdate,changedate,postingdate, modelid, company)
    values('0000','Dummy','Dummy', '','2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z',300,'1000' );

insert into vat
(id, name, description, percent, input_vat_account, output_vat_account, postingdate, changedate, enterdate,  company, modelid)
values
    ('v101','Dummy','Dummy',0.07, '0650', '0651', '2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z', '2018-01-01T00:00:00.00Z', '1000',14),
    ('4711','myFirstVat','myFirstVat',1, '1406', '3806', current_timestamp, current_timestamp, current_timestamp, '1000',14);


insert into users(user_name, first_name, last_name,  hash, email, phone, department, menu, company, modelid)
values('jdegoes011','John','dgoes', '$21a$10$0IZtq3wGiRQSMIuoIgNKrePjQfmGFRgkpnHwyY9RcbUhZxU9Ha1mCX', 'whatYourGrandma1Name@gmail.com'
   , '11-4711-0123','Admin', '1300,9,1120,3,14,1000,18,1,112,106,11,6,10', '1000',111),
   ('myUserName','myUserFirstName','myUserLastName', 'hash1', 'myEmail@email.com', '+49-1111-11100','Accountant', '1,10', '1000',111);

