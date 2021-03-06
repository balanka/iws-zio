create table customers
(
    id uuid not null primary key,
    first_name varchar not null,
    last_name varchar not null,
    verified boolean not null,
    dob date not null
);

create table orders
(
    id uuid not null primary key,
    customer_id uuid not null,
    order_date date not null
);

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
    bankcode    varchar(50),
    amount      numeric(12, 2),
    currency    varchar(200),
    info        varchar(250),
    company     varchar(50),
    companyiban varchar(50),
    posted      boolean,
    modelid     integer);

create table  bank
(
    id            varchar(50)                    not null
    primary key,
    name          varchar(255)                   not null,
    description   varchar(255),
    posting_date  timestamp default CURRENT_DATE not null,
    modified_date timestamp default CURRENT_DATE not null,
    enter_date    timestamp default CURRENT_DATE not null,
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
    inputvataccount  varchar(50)                         not null,
    outputvataccount varchar(50)                         not null,
    posting_date     timestamp      default CURRENT_DATE not null,
    modified_date    timestamp      default CURRENT_DATE not null,
    enter_date       timestamp      default CURRENT_DATE not null,
    company          varchar(50)                         not null,
    modelid          integer                             not null
    );
create table  account
(
    id            varchar(50)                         not null
    primary key,
    name          varchar(255)                        not null,
    description   varchar(255),
    posting_date  timestamp      default CURRENT_DATE not null,
    modified_date timestamp      default CURRENT_DATE not null,
    enter_date    timestamp      default CURRENT_DATE not null,
    company       varchar(50)                         not null,
    modelid       integer        default 9            not null,
    account       varchar(50),
    isdebit       boolean        default true         not null,
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

create table if not exists master_compta
(
    id           bigint                            not null
    primary key,
    oid          bigint                            not null,
    costcenter   varchar(50)                       not null,
    account      varchar(50)                       not null,
    headertext   varchar(380) default ''::character varying,
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
create table if not exists details_compta
(
    id       bigint                                      not null
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
    file_content integer default 0,
    journal_type integer default 0,
    idebit       numeric(12, 2),
    debit        numeric(12, 2),
    icredit      numeric(12, 2),
    credit       numeric(12, 2),
    side         boolean
    );

insert into customers
    (id, first_name, last_name, verified, dob)
values
    ('60b01fc9-c902-4468-8d49-3c0f989def37', 'Ronald', 'Russell', true, '1983-01-05'),
    ('f76c9ace-be07-4bf3-bd4c-4a9c62882e64', 'Terrence', 'Noel', true, '1999-11-02'),
    ('784426a5-b90a-4759-afbb-571b7a0ba35e', 'Mila', 'Paterso', true, '1990-11-16'),
    ('df8215a2-d5fd-4c6c-9984-801a1b3a2a0b', 'Alana', 'Murray', true, '1995-11-12'),
    ('636ae137-5b1a-4c8c-b11f-c47c624d9cdc', 'Jose', 'Wiggins', false, '1987-03-23');

insert into orders
    (id, customer_id, order_date)
values
    ('04912093-cc2e-46ac-b64c-1bd7bb7758c3', '60b01fc9-c902-4468-8d49-3c0f989def37', '2019-03-25'),
    ('a243fa42-817a-44ec-8b67-22193d212d82', '60b01fc9-c902-4468-8d49-3c0f989def37', '2018-06-04'),
    ('9022dd0d-06d6-4a43-9121-2993fc7712a1', 'df8215a2-d5fd-4c6c-9984-801a1b3a2a0b', '2019-08-19'),
    ('38d66d44-3cfa-488a-ac77-30277751418f', '636ae137-5b1a-4c8c-b11f-c47c624d9cdc', '2019-08-30'),
    ('7b2627d5-0150-44df-9171-3462e20797ee', '636ae137-5b1a-4c8c-b11f-c47c624d9cdc', '2019-03-07'),
    ('62cd4109-3e5d-40cc-8188-3899fc1f8bdf', '60b01fc9-c902-4468-8d49-3c0f989def37', '2020-03-19'),
    ('9473a0bc-396a-4936-96b0-3eea922af36b', 'df8215a2-d5fd-4c6c-9984-801a1b3a2a0b', '2020-05-11'),
    ('b8bac18d-769f-48ed-809d-4b6c0e4d1795', 'df8215a2-d5fd-4c6c-9984-801a1b3a2a0b', '2019-02-21'),
    ('852e2dc9-4ec3-4225-a6f7-4f42f8ff728e', '60b01fc9-c902-4468-8d49-3c0f989def37', '2018-05-06'),
    ('bebbfe4d-4ec3-4389-bdc2-50e9eac2b15b', '784426a5-b90a-4759-afbb-571b7a0ba35e', '2019-02-11'),
    ('742d45a0-e81a-41ce-95ad-55b4cabba258', 'f76c9ace-be07-4bf3-bd4c-4a9c62882e64', '2019-10-12'),
    ('618aa21f-700b-4ca7-933c-67066cf4cd97', '60b01fc9-c902-4468-8d49-3c0f989def37', '2019-01-29'),
    ('606da090-dd33-4a77-8746-6ed0e8443ab2', 'f76c9ace-be07-4bf3-bd4c-4a9c62882e64', '2019-02-10'),
    ('4914028d-2e28-4033-a5f2-8f4fcdee8206', '60b01fc9-c902-4468-8d49-3c0f989def37', '2019-09-27'),
    ('d4e77298-d829-4e36-a6a0-902403f4b7d3', 'df8215a2-d5fd-4c6c-9984-801a1b3a2a0b', '2018-11-13'),
    ('fd0fa8d4-e1a0-4369-be07-945450db5d36', '636ae137-5b1a-4c8c-b11f-c47c624d9cdc', '2020-01-15'),
    ('d6d8dddc-4b0b-4d74-8edc-a54e9b7f35f7', 'f76c9ace-be07-4bf3-bd4c-4a9c62882e64', '2018-07-10'),
    ('876b6034-b33c-4497-81ee-b4e8742164c2', '784426a5-b90a-4759-afbb-571b7a0ba35e', '2019-08-01'),
    ('91caa28a-a5fe-40d7-979c-bd6a128d0418', 'df8215a2-d5fd-4c6c-9984-801a1b3a2a0b', '2019-12-08'),
    ('401c7ab1-41cf-4756-8af5-be25cf2ae67b', '784426a5-b90a-4759-afbb-571b7a0ba35e', '2019-11-04'),
    ('2c3fc180-d0df-4d7b-a271-e6ccd2440393', '784426a5-b90a-4759-afbb-571b7a0ba35e', '2018-10-14'),
    ('763a7c39-833f-4ee8-9939-e80dfdbfc0fc', 'f76c9ace-be07-4bf3-bd4c-4a9c62882e64', '2020-04-05'),
    ('5011d206-8eff-42c4-868e-f1a625e1f186', '636ae137-5b1a-4c8c-b11f-c47c624d9cdc', '2019-01-23'),
    ('0a48ffb0-ec61-4147-af56-fc4dbca8de0a', 'f76c9ace-be07-4bf3-bd4c-4a9c62882e64', '2019-05-14'),
    ('5883cb62-d792-4ee3-acbc-fe85b6baa998', '784426a5-b90a-4759-afbb-571b7a0ba35e', '2020-04-30');
insert into account
(id, name, description, posting_date, modified_date, enter_date,  company, modelid,account,
 isdebit, balancesheet, idebit, icredit, debit, credit, currency) values
 ('9900','Bilanz','Bilanz ',current_timestamp, current_timestamp, current_timestamp, '1000',11
     , '', true, true, 0.0, 0.0, 0.0, 0.0, 'EUR'),
 ('9901','Bilanz Aktiva','Bilanz Aktiva',current_timestamp, current_timestamp, current_timestamp, '1000',11
     , '9900', true, true, 0.0, 0.0, 0.0, 0.0, 'EUR'),
  ('9902','Bilanz Passiva','Bilanz Passiva',current_timestamp, current_timestamp, current_timestamp, '1000',11
     , '9900', false, true, 0.0, 0.0, 0.0, 0.0, 'EUR'),
 ('1200','Forderungen aus Lieferungen und Leistungen','Forderung a. L & L',current_timestamp, current_timestamp, current_timestamp, '1000',11
  , '9900', true, true, 0.0, 0.0, 0.0, 0.0, 'EUR'),
  ('1800','Bank','Bank',current_timestamp, current_timestamp, current_timestamp, '1000',11
  , '1800', true, true, 0.0, 0.0, 0.0, 0.0, 'EUR'),
    ('1810','Giro SPK Bielefeld','Giro SPK Bielefeld ',current_timestamp, current_timestamp, current_timestamp, '1000',11
  , '1800', true, true, 0.0, 0.0, 0.0, 0.0, 'EUR');

insert into periodic_account_balance
(id, account, period, idebit,debit,icredit,credit, company,currency,modelid)
values(CONCAT(to_char( CURRENT_DATE- INTERVAL '1 year', 'YYYYMM'),'1200'), '1200', TO_NUMBER(to_char( CURRENT_DATE- INTERVAL '1 year', 'YYYYMM'),'99999999'), 0, 1000, 0, 0
,'1000' , 'EUR', 106),
(CONCAT(to_char( CURRENT_DATE- INTERVAL '1 year', 'YYYY'),'001200'), '1200', TO_NUMBER(CONCAT(to_char( CURRENT_DATE- INTERVAL '1 year', 'YYYY'),'00'),'99999999'), 0, 1000, 0, 0
      ,'1000' , 'EUR', 106),
(CONCAT(to_char( CURRENT_DATE, 'YYYYMM'),'1200'), '1200', TO_NUMBER(to_char( CURRENT_DATE, 'YYYYMM'),'99999999'), 0, 0, 0, 0
      ,'1000' , 'EUR', 106);;


 insert into bankstatement
 (id,depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bankCode,amount, currency, info, company, companyIban, posted, modelid)
values
(4711, 'B Mady',current_timestamp, current_timestamp,'TEST POSTING','TEST PURPOSE','B Mady','430000000ACTNO','43007711BIC', 1000, 'EUR','INFO TXT','1000','47114300IBAN',false,18 );

insert into bank
(id, name, description, posting_date, modified_date, enter_date,  company, modelid)
values('4711','myFirstBank','myFirstBank',current_timestamp, current_timestamp, current_timestamp, '1000',11);

insert into vat
(id, name, description, percent, inputvataccount, outputvataccount, posting_date, modified_date, enter_date,  company, modelid)
values
    ('4711','myFirstVat','myFirstVat',1, '1406', '3806', current_timestamp, current_timestamp, current_timestamp, '1000',6)