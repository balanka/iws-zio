--
-- NOTE:
--
-- File paths need to be edited. Search for $$PATH$$ and
-- replace it with the path to the directory containing
-- the extracted data files.
--
--
-- PostgreSQL database dump
--

-- Dumped from database version 15.2
-- Dumped by pg_dump version 15.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

DROP DATABASE "iwsN";
--
-- Name: iwsN; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE "iwsN" WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE_PROVIDER = libc LOCALE = 'C';


ALTER DATABASE "iwsN" OWNER TO postgres;

\connect "iwsN"

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: account; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.account (
    id character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    changedate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer DEFAULT 9 NOT NULL,
    account character varying(50),
    is_debit boolean DEFAULT true NOT NULL,
    balancesheet boolean DEFAULT false NOT NULL,
    idebit numeric(12,2) DEFAULT 0,
    icredit numeric(12,2) DEFAULT 0,
    debit numeric(12,2) DEFAULT 0,
    credit numeric(12,2) DEFAULT 0,
    currency character varying(5) DEFAULT ''::character varying
);


ALTER TABLE public.account OWNER TO postgres;

--
-- Name: account_SQL; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."account_SQL" (
    id character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    posting_date timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    modified_date timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enter_date timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer DEFAULT 9 NOT NULL,
    account character varying(50),
    isdebit boolean DEFAULT true NOT NULL,
    balancesheet boolean DEFAULT false NOT NULL,
    idebit numeric(12,2) DEFAULT 0,
    icredit numeric(12,2) DEFAULT 0,
    debit numeric(12,2) DEFAULT 0,
    credit numeric(12,2) DEFAULT 0,
    currency character varying(5) DEFAULT 'EUR'::character varying
);


ALTER TABLE public."account_SQL" OWNER TO postgres;

--
-- Name: accounts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.accounts (
    id character varying NOT NULL,
    name character varying NOT NULL,
    description character varying NOT NULL,
    modelid bigint NOT NULL,
    parent character varying NOT NULL
);


ALTER TABLE public.accounts OWNER TO postgres;

--
-- Name: article; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.article (
    id character varying NOT NULL,
    name character varying NOT NULL,
    description character varying NOT NULL,
    modelid bigint NOT NULL,
    price numeric(10,2),
    parent character varying NOT NULL,
    stocked boolean
);


ALTER TABLE public.article OWNER TO postgres;

--
-- Name: asset; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.asset (
    id character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    changedate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer NOT NULL,
    account character varying(50),
    oaccount character varying(50),
    scrap_value numeric(12,2),
    life_span integer,
    dep_method integer,
    rate numeric(5,2),
    frequency integer,
    currency character varying(50) NOT NULL
);


ALTER TABLE public.asset OWNER TO postgres;

--
-- Name: bank; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bank (
    id character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    changedate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer NOT NULL
);


ALTER TABLE public.bank OWNER TO postgres;

--
-- Name: bankaccount; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bankaccount (
    id character varying(50) NOT NULL,
    owner character varying(255) NOT NULL,
    bic character varying(50) NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer NOT NULL
);


ALTER TABLE public.bankaccount OWNER TO postgres;

--
-- Name: bankstatement; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bankstatement (
    id bigint NOT NULL,
    depositor character varying(50),
    postingdate timestamp without time zone,
    valuedate timestamp without time zone,
    postingtext character varying(300),
    purpose character varying(350),
    beneficiary character varying(250),
    accountno character varying(50),
    bank_code character varying(50),
    amount numeric(12,2),
    currency character varying(200),
    info character varying(250),
    company character varying(50),
    company_iban character varying(50),
    posted boolean,
    modelid integer NOT NULL,
    period integer
);


ALTER TABLE public.bankstatement OWNER TO postgres;

--
-- Name: bankstatement2_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.bankstatement2_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.bankstatement2_id_seq OWNER TO postgres;

--
-- Name: bankstatement2_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.bankstatement2_id_seq OWNED BY public.bankstatement.id;


--
-- Name: company; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.company (
    id character varying NOT NULL,
    name character varying NOT NULL,
    street character varying,
    city character varying,
    state character varying,
    zip character varying,
    bank_acc character varying NOT NULL,
    purchasingclearingacc character varying NOT NULL,
    salesclearingacc character varying NOT NULL,
    paymentclearingacc character varying NOT NULL,
    settlementclearingacc character varying NOT NULL,
    tax_code character varying NOT NULL,
    vat_code character varying NOT NULL,
    currency character varying NOT NULL,
    balance_sheet_acc character varying NOT NULL,
    income_stmt_acc character varying NOT NULL,
    cashacc character varying NOT NULL,
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    changedate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    modelid integer NOT NULL,
    pageheadertext character varying(350),
    pagefootertext character varying(350),
    headertext character varying(350),
    footertext character varying(350),
    logocontent character varying,
    logoname character varying,
    contenttype character varying,
    partner character varying,
    phone character varying,
    fax character varying,
    email character varying,
    locale character varying,
    description character varying,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE,
    country character varying DEFAULT 'DE'::character varying,
    iban character varying
);


ALTER TABLE public.company OWNER TO postgres;

--
-- Name: costcenter; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.costcenter (
    id character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    account character varying(50) NOT NULL,
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    changedate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer DEFAULT 6 NOT NULL
);


ALTER TABLE public.costcenter OWNER TO postgres;

--
-- Name: currency; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.currency (
    id character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    posting_date date DEFAULT CURRENT_DATE NOT NULL,
    modified_date date DEFAULT CURRENT_DATE NOT NULL,
    enter_date date DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer DEFAULT 5 NOT NULL
);


ALTER TABLE public.currency OWNER TO postgres;

--
-- Name: customer; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.customer (
    id character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255) NOT NULL,
    street character varying(255),
    city character varying(255),
    state character varying(255),
    zip character varying(255),
    phone character varying(50),
    email character varying(50),
    account character varying(50),
    iban character varying(50),
    vatcode character varying(50),
    oaccount character varying(50),
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    changedate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer DEFAULT 3 NOT NULL,
    country character varying(50) DEFAULT ''::character varying
);


ALTER TABLE public.customer OWNER TO postgres;

--
-- Name: details_compta; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.details_compta (
    id bigint DEFAULT nextval('public.details_compta_id_seq'::regclass) NOT NULL,
    transid bigint DEFAULT 0 NOT NULL,
    account character varying(50) DEFAULT NULL::character varying NOT NULL,
    side boolean NOT NULL,
    oaccount character varying(50) NOT NULL,
    amount numeric(12,2) NOT NULL,
    duedate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    text character varying(380),
    currency character varying(10) NOT NULL,
    terms character varying(250),
    posted boolean DEFAULT false,
    company character varying(50) DEFAULT 1000,
    account_name character varying,
    oaccount_name character varying
);


ALTER TABLE public.details_compta OWNER TO postgres;

--
-- Name: dual; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.dual (
    column1 boolean
);


ALTER TABLE public.dual OWNER TO postgres;

--
-- Name: file_content; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.file_content (
    id integer NOT NULL,
    transid integer NOT NULL,
    filename character varying(255),
    filecontent character varying(3500),
    contenttype character varying(50)
);


ALTER TABLE public.file_content OWNER TO postgres;

--
-- Name: fmodule; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fmodule (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255) NOT NULL,
    transdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    account character varying(50) NOT NULL,
    is_debit boolean,
    company character varying(50) NOT NULL,
    modelid integer NOT NULL
);


ALTER TABLE public.fmodule OWNER TO postgres;

--
-- Name: journal; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.journal (
    id bigint DEFAULT nextval('public.journal_id_seq'::regclass) NOT NULL,
    transid bigint NOT NULL,
    oid bigint,
    account character varying(50) NOT NULL,
    oaccount character varying(50) NOT NULL,
    transdate timestamp without time zone NOT NULL,
    enterdate timestamp without time zone NOT NULL,
    postingdate timestamp without time zone NOT NULL,
    period integer NOT NULL,
    amount numeric(12,2),
    company character varying(50) NOT NULL,
    currency character varying(50) NOT NULL,
    text character varying(255) NOT NULL,
    month integer,
    year integer NOT NULL,
    modelid integer NOT NULL,
    file_content integer DEFAULT 0,
    journal_type integer DEFAULT 0,
    idebit numeric(12,2),
    debit numeric(12,2),
    icredit numeric(12,2),
    credit numeric(12,2),
    side boolean
);


ALTER TABLE public.journal OWNER TO postgres;

--
-- Name: master_compta; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.master_compta (
    id bigint DEFAULT nextval('public.master_compta_id_seq'::regclass) NOT NULL,
    oid bigint NOT NULL,
    costcenter character varying(50) NOT NULL,
    account character varying(50) NOT NULL,
    text character varying(380) DEFAULT ''::character varying,
    transdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    file_content integer DEFAULT '-1'::integer,
    posted boolean DEFAULT false,
    modelid integer NOT NULL,
    period integer,
    type_journal integer DEFAULT 0,
    id1 bigint DEFAULT 0
);


ALTER TABLE public.master_compta OWNER TO postgres;

--
-- Name: master_comptakopie; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.master_comptakopie (
    id bigint NOT NULL,
    oid bigint NOT NULL,
    costcenter character varying(50) NOT NULL,
    account character varying(50) NOT NULL,
    headertext character varying(380) DEFAULT ''::character varying,
    transdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    file_content integer DEFAULT '-1'::integer,
    posted boolean DEFAULT false,
    modelid integer NOT NULL,
    period integer,
    type_journal integer DEFAULT 0
);


ALTER TABLE public.master_comptakopie OWNER TO postgres;

--
-- Name: masterfiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.masterfiles (
    id character varying DEFAULT '0'::character varying NOT NULL,
    name character varying NOT NULL,
    description character varying NOT NULL,
    modelid bigint NOT NULL,
    parent character varying NOT NULL
);


ALTER TABLE public.masterfiles OWNER TO postgres;

--
-- Name: module; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.module (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    changedate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer DEFAULT 6 NOT NULL,
    path character varying(255) DEFAULT '/'::character varying,
    parent integer DEFAULT '-1'::integer NOT NULL
);


ALTER TABLE public.module OWNER TO postgres;

--
-- Name: periodic_account_balance; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.periodic_account_balance (
    account character varying(50) NOT NULL,
    period integer NOT NULL,
    idebit numeric(12,2) DEFAULT 0 NOT NULL,
    icredit numeric(12,2) DEFAULT 0 NOT NULL,
    debit numeric(12,2) DEFAULT 0 NOT NULL,
    credit numeric(12,2) DEFAULT 0 NOT NULL,
    company character varying(50) NOT NULL,
    currency character varying(50) NOT NULL,
    modelid integer DEFAULT 106 NOT NULL,
    id character varying(50) NOT NULL,
    name character varying
);


ALTER TABLE public.periodic_account_balance OWNER TO postgres;

--
-- Name: permission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.permission (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255) NOT NULL,
    transdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    short character varying(10) NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer DEFAULT 141
);


ALTER TABLE public.permission OWNER TO postgres;

--
-- Name: role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.role (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255) NOT NULL,
    transdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer DEFAULT 121
);


ALTER TABLE public.role OWNER TO postgres;

--
-- Name: routes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.routes (
    id character varying NOT NULL,
    name character varying NOT NULL,
    description character varying NOT NULL,
    modelid bigint NOT NULL,
    component character varying NOT NULL,
    exact integer DEFAULT 1
);


ALTER TABLE public.routes OWNER TO postgres;

--
-- Name: supplier; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.supplier (
    id character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255) NOT NULL,
    street character varying(255),
    city character varying(255),
    state character varying(255),
    zip character varying(255),
    phone character varying(50),
    email character varying(50),
    account character varying(50),
    iban character varying(50),
    vatcode character varying(50),
    oaccount character varying(50),
    postingdate date DEFAULT CURRENT_DATE NOT NULL,
    changedate date DEFAULT CURRENT_DATE NOT NULL,
    enterdate date DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer DEFAULT 5 NOT NULL,
    country character varying(50) DEFAULT 'X'::character varying NOT NULL
);


ALTER TABLE public.supplier OWNER TO postgres;

--
-- Name: user_right; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_right (
    moduleid integer NOT NULL,
    roleid integer NOT NULL,
    short character varying(50) NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer DEFAULT 131
);


ALTER TABLE public.user_right OWNER TO postgres;

--
-- Name: user_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_role (
    userid integer NOT NULL,
    roleid integer NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer DEFAULT 161
);


ALTER TABLE public.user_role OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id integer NOT NULL,
    user_name character varying NOT NULL,
    first_name character varying NOT NULL,
    last_name character varying NOT NULL,
    email character varying NOT NULL,
    hash character varying NOT NULL,
    phone character varying NOT NULL,
    department character varying DEFAULT 'Customer'::character varying NOT NULL,
    company character varying(20) DEFAULT '1000'::character varying NOT NULL,
    modelid integer DEFAULT 111,
    menu character varying
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.users ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: vat; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.vat (
    id character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    percent numeric(12,2) DEFAULT 0 NOT NULL,
    input_vat_account character varying(50) NOT NULL,
    output_vat_account character varying(50) NOT NULL,
    postingdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    changedate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    enterdate timestamp without time zone DEFAULT CURRENT_DATE NOT NULL,
    company character varying(50) NOT NULL,
    modelid integer NOT NULL
);


ALTER TABLE public.vat OWNER TO postgres;

--
-- Name: bankstatement id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bankstatement ALTER COLUMN id SET DEFAULT nextval('public.bankstatement2_id_seq'::regclass);


--
-- Data for Name: account; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.account (id, name, description, postingdate, changedate, enterdate, company, modelid, account, is_debit, balancesheet, idebit, icredit, debit, credit, currency) FROM stdin;
\.
COPY public.account (id, name, description, postingdate, changedate, enterdate, company, modelid, account, is_debit, balancesheet, idebit, icredit, debit, credit, currency) FROM '$$PATH$$/3932.dat';

--
-- Data for Name: account_SQL; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public."account_SQL" (id, name, description, posting_date, modified_date, enter_date, company, modelid, account, isdebit, balancesheet, idebit, icredit, debit, credit, currency) FROM stdin;
\.
COPY public."account_SQL" (id, name, description, posting_date, modified_date, enter_date, company, modelid, account, isdebit, balancesheet, idebit, icredit, debit, credit, currency) FROM '$$PATH$$/3933.dat';

--
-- Data for Name: accounts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.accounts (id, name, description, modelid, parent) FROM stdin;
\.
COPY public.accounts (id, name, description, modelid, parent) FROM '$$PATH$$/3934.dat';

--
-- Data for Name: article; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.article (id, name, description, modelid, price, parent, stocked) FROM stdin;
\.
COPY public.article (id, name, description, modelid, price, parent, stocked) FROM '$$PATH$$/3935.dat';

--
-- Data for Name: asset; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.asset (id, name, description, changedate, enterdate, postingdate, company, modelid, account, oaccount, scrap_value, life_span, dep_method, rate, frequency, currency) FROM stdin;
\.
COPY public.asset (id, name, description, changedate, enterdate, postingdate, company, modelid, account, oaccount, scrap_value, life_span, dep_method, rate, frequency, currency) FROM '$$PATH$$/3963.dat';

--
-- Data for Name: bank; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bank (id, name, description, postingdate, changedate, enterdate, company, modelid) FROM stdin;
\.
COPY public.bank (id, name, description, postingdate, changedate, enterdate, company, modelid) FROM '$$PATH$$/3936.dat';

--
-- Data for Name: bankaccount; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bankaccount (id, owner, bic, company, modelid) FROM stdin;
\.
COPY public.bankaccount (id, owner, bic, company, modelid) FROM '$$PATH$$/3937.dat';

--
-- Data for Name: bankstatement; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bankstatement (id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bank_code, amount, currency, info, company, company_iban, posted, modelid, period) FROM stdin;
\.
COPY public.bankstatement (id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bank_code, amount, currency, info, company, company_iban, posted, modelid, period) FROM '$$PATH$$/3938.dat';

--
-- Data for Name: company; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.company (id, name, street, city, state, zip, bank_acc, purchasingclearingacc, salesclearingacc, paymentclearingacc, settlementclearingacc, tax_code, vat_code, currency, balance_sheet_acc, income_stmt_acc, cashacc, postingdate, changedate, modelid, pageheadertext, pagefootertext, headertext, footertext, logocontent, logoname, contenttype, partner, phone, fax, email, locale, description, enterdate, country, iban) FROM stdin;
\.
COPY public.company (id, name, street, city, state, zip, bank_acc, purchasingclearingacc, salesclearingacc, paymentclearingacc, settlementclearingacc, tax_code, vat_code, currency, balance_sheet_acc, income_stmt_acc, cashacc, postingdate, changedate, modelid, pageheadertext, pagefootertext, headertext, footertext, logocontent, logoname, contenttype, partner, phone, fax, email, locale, description, enterdate, country, iban) FROM '$$PATH$$/3940.dat';

--
-- Data for Name: costcenter; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.costcenter (id, name, description, account, postingdate, changedate, enterdate, company, modelid) FROM stdin;
\.
COPY public.costcenter (id, name, description, account, postingdate, changedate, enterdate, company, modelid) FROM '$$PATH$$/3941.dat';

--
-- Data for Name: currency; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.currency (id, name, description, posting_date, modified_date, enter_date, company, modelid) FROM stdin;
\.
COPY public.currency (id, name, description, posting_date, modified_date, enter_date, company, modelid) FROM '$$PATH$$/3942.dat';

--
-- Data for Name: customer; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.customer (id, name, description, street, city, state, zip, phone, email, account, iban, vatcode, oaccount, postingdate, changedate, enterdate, company, modelid, country) FROM stdin;
\.
COPY public.customer (id, name, description, street, city, state, zip, phone, email, account, iban, vatcode, oaccount, postingdate, changedate, enterdate, company, modelid, country) FROM '$$PATH$$/3943.dat';

--
-- Data for Name: details_compta; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.details_compta (id, transid, account, side, oaccount, amount, duedate, text, currency, terms, posted, company, account_name, oaccount_name) FROM stdin;
\.
COPY public.details_compta (id, transid, account, side, oaccount, amount, duedate, text, currency, terms, posted, company, account_name, oaccount_name) FROM '$$PATH$$/3944.dat';

--
-- Data for Name: dual; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.dual (column1) FROM stdin;
\.
COPY public.dual (column1) FROM '$$PATH$$/3945.dat';

--
-- Data for Name: file_content; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.file_content (id, transid, filename, filecontent, contenttype) FROM stdin;
\.
COPY public.file_content (id, transid, filename, filecontent, contenttype) FROM '$$PATH$$/3946.dat';

--
-- Data for Name: fmodule; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.fmodule (id, name, description, transdate, enterdate, postingdate, account, is_debit, company, modelid) FROM stdin;
\.
COPY public.fmodule (id, name, description, transdate, enterdate, postingdate, account, is_debit, company, modelid) FROM '$$PATH$$/3960.dat';

--
-- Data for Name: journal; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.journal (id, transid, oid, account, oaccount, transdate, enterdate, postingdate, period, amount, company, currency, text, month, year, modelid, file_content, journal_type, idebit, debit, icredit, credit, side) FROM stdin;
\.
COPY public.journal (id, transid, oid, account, oaccount, transdate, enterdate, postingdate, period, amount, company, currency, text, month, year, modelid, file_content, journal_type, idebit, debit, icredit, credit, side) FROM '$$PATH$$/3947.dat';

--
-- Data for Name: master_compta; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.master_compta (id, oid, costcenter, account, text, transdate, postingdate, enterdate, company, file_content, posted, modelid, period, type_journal, id1) FROM stdin;
\.
COPY public.master_compta (id, oid, costcenter, account, text, transdate, postingdate, enterdate, company, file_content, posted, modelid, period, type_journal, id1) FROM '$$PATH$$/3948.dat';

--
-- Data for Name: master_comptakopie; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.master_comptakopie (id, oid, costcenter, account, headertext, transdate, postingdate, enterdate, company, file_content, posted, modelid, period, type_journal) FROM stdin;
\.
COPY public.master_comptakopie (id, oid, costcenter, account, headertext, transdate, postingdate, enterdate, company, file_content, posted, modelid, period, type_journal) FROM '$$PATH$$/3949.dat';

--
-- Data for Name: masterfiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.masterfiles (id, name, description, modelid, parent) FROM stdin;
\.
COPY public.masterfiles (id, name, description, modelid, parent) FROM '$$PATH$$/3950.dat';

--
-- Data for Name: module; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.module (id, name, description, postingdate, changedate, enterdate, company, modelid, path, parent) FROM stdin;
\.
COPY public.module (id, name, description, postingdate, changedate, enterdate, company, modelid, path, parent) FROM '$$PATH$$/3951.dat';

--
-- Data for Name: periodic_account_balance; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.periodic_account_balance (account, period, idebit, icredit, debit, credit, company, currency, modelid, id, name) FROM stdin;
\.
COPY public.periodic_account_balance (account, period, idebit, icredit, debit, credit, company, currency, modelid, id, name) FROM '$$PATH$$/3952.dat';

--
-- Data for Name: permission; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.permission (id, name, description, transdate, enterdate, postingdate, short, company, modelid) FROM stdin;
\.
COPY public.permission (id, name, description, transdate, enterdate, postingdate, short, company, modelid) FROM '$$PATH$$/3959.dat';

--
-- Data for Name: role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.role (id, name, description, transdate, enterdate, postingdate, company, modelid) FROM stdin;
\.
COPY public.role (id, name, description, transdate, enterdate, postingdate, company, modelid) FROM '$$PATH$$/3961.dat';

--
-- Data for Name: routes; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.routes (id, name, description, modelid, component, exact) FROM stdin;
\.
COPY public.routes (id, name, description, modelid, component, exact) FROM '$$PATH$$/3953.dat';

--
-- Data for Name: supplier; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.supplier (id, name, description, street, city, state, zip, phone, email, account, iban, vatcode, oaccount, postingdate, changedate, enterdate, company, modelid, country) FROM stdin;
\.
COPY public.supplier (id, name, description, street, city, state, zip, phone, email, account, iban, vatcode, oaccount, postingdate, changedate, enterdate, company, modelid, country) FROM '$$PATH$$/3954.dat';

--
-- Data for Name: user_right; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_right (moduleid, roleid, short, company, modelid) FROM stdin;
\.
COPY public.user_right (moduleid, roleid, short, company, modelid) FROM '$$PATH$$/3958.dat';

--
-- Data for Name: user_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_role (userid, roleid, company, modelid) FROM stdin;
\.
COPY public.user_role (userid, roleid, company, modelid) FROM '$$PATH$$/3962.dat';

--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, user_name, first_name, last_name, email, hash, phone, department, company, modelid, menu) FROM stdin;
\.
COPY public.users (id, user_name, first_name, last_name, email, hash, phone, department, company, modelid, menu) FROM '$$PATH$$/3955.dat';

--
-- Data for Name: vat; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.vat (id, name, description, percent, input_vat_account, output_vat_account, postingdate, changedate, enterdate, company, modelid) FROM stdin;
\.
COPY public.vat (id, name, description, percent, input_vat_account, output_vat_account, postingdate, changedate, enterdate, company, modelid) FROM '$$PATH$$/3957.dat';

--
-- Name: bankstatement2_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.bankstatement2_id_seq', 3038, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 6, true);


--
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);


--
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);


--
-- Name: account_SQL accountx_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."account_SQL"
    ADD CONSTRAINT accountx_pkey PRIMARY KEY (id);


--
-- Name: article article_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.article
    ADD CONSTRAINT article_pkey PRIMARY KEY (id);


--
-- Name: asset asset_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.asset
    ADD CONSTRAINT asset_pkey PRIMARY KEY (id);


--
-- Name: bank bank_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bank
    ADD CONSTRAINT bank_pkey PRIMARY KEY (id);


--
-- Name: bankaccount bankaccount_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bankaccount
    ADD CONSTRAINT bankaccount_unique UNIQUE (owner, id);


--
-- Name: bankstatement bankstatement2_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bankstatement
    ADD CONSTRAINT bankstatement2_pkey PRIMARY KEY (id);


--
-- Name: company company_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT company_pkey PRIMARY KEY (id);


--
-- Name: costcenter costcenter_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.costcenter
    ADD CONSTRAINT costcenter_pkey PRIMARY KEY (id);


--
-- Name: currency currency_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.currency
    ADD CONSTRAINT currency_pkey PRIMARY KEY (id);


--
-- Name: customer customer_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (id);


--
-- Name: details_compta detailcompta_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.details_compta
    ADD CONSTRAINT detailcompta_pkey PRIMARY KEY (id);


--
-- Name: file_content file_content_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_content
    ADD CONSTRAINT file_content_pkey PRIMARY KEY (id);


--
-- Name: fmodule fmodule_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fmodule
    ADD CONSTRAINT fmodule_pk PRIMARY KEY (id, company);


--
-- Name: journal journal_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.journal
    ADD CONSTRAINT journal_pkey PRIMARY KEY (id);


--
-- Name: master_compta master_compta_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.master_compta
    ADD CONSTRAINT master_compta_pkey PRIMARY KEY (id);


--
-- Name: master_comptakopie master_comptakopie_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.master_comptakopie
    ADD CONSTRAINT master_comptakopie_pkey PRIMARY KEY (id);


--
-- Name: masterfiles masterfiles_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.masterfiles
    ADD CONSTRAINT masterfiles_pk PRIMARY KEY (id);


--
-- Name: module module_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.module
    ADD CONSTRAINT module_pk PRIMARY KEY (id, company);


--
-- Name: periodic_account_balance periodic_account_balance_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.periodic_account_balance
    ADD CONSTRAINT periodic_account_balance_pk PRIMARY KEY (id);


--
-- Name: permission permission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permission
    ADD CONSTRAINT permission_pkey PRIMARY KEY (id);


--
-- Name: role role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id, company);


--
-- Name: routes route_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.routes
    ADD CONSTRAINT route_pkey PRIMARY KEY (id);


--
-- Name: supplier supplier_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.supplier
    ADD CONSTRAINT supplier_pkey PRIMARY KEY (id);


--
-- Name: user_role user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_pkey PRIMARY KEY (userid, roleid, company);


--
-- Name: user_right userright_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_right
    ADD CONSTRAINT userright_pkey PRIMARY KEY (moduleid, roleid, short, company);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_user_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_user_name_key UNIQUE (user_name);


--
-- Name: vat vat_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vat
    ADD CONSTRAINT vat_pkey PRIMARY KEY (id);


--
-- Name: bankaccount_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX bankaccount_idx ON public.bankaccount USING btree (id, owner);


--
-- Name: masterfiles_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX masterfiles_id_uindex ON public.masterfiles USING btree (id);


--
-- PostgreSQL database dump complete
--

