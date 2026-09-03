

insert into company (id, name, street, city, state, zip, bank_acc, purchasing_clearing_acc, sales_clearing_acc, payment_clearing_acc, settlement_clearing_acc, tax_code, vat_code, currency, balance_sheet_acc, income_stmt_acc, cash_acc, postingdate, changedate, modelid, pageheadertext, pagefootertext, headertext, footertext, logocontent, logoname, contenttype, partner, phone, fax, email, locale, description, enterdate, country, iban, account, oaccount)
select '5700', 'SimGUI SARLU', street, city, state, zip, bank_acc, purchasing_clearing_acc, sales_clearing_acc, payment_clearing_acc, settlement_clearing_acc, tax_code, vat_code, currency, balance_sheet_acc, income_stmt_acc, cash_acc, postingdate, changedate, modelid, pageheadertext, pagefootertext, headertext, footertext, logocontent, logoname, contenttype, partner, phone, fax, email, locale, description, enterdate, country, iban, account, oaccount from company where id='5600';
create sequence master_compta_id_seq_5700 start with 1;
alter sequence master_compta_id_seq_5700 owner to postgres;
create sequence public.details_compta_id_seq_5700 start with 1;
alter sequence public.details_compta_id_seq_5700 owner to postgres;
create sequence public.transaction_id_seq_5700 start with 1;
alter sequence public.transaction_id_seq_5700 owner to postgres;
create sequence public.transaction_details_id_seq_5700 start with 1;
alter sequence public.transaction_details_id_seq_5700 owner to postgres;
create sequence public.transaction_log_id_seq_5700 start with 1;
alter sequence public.transaction_log_id_seq_5700 owner to postgres;
create sequence public.journal_id_seq_5700 start with 1;
alter sequence public.journal_id_seq_5700 owner to postgres;
create sequence public.bankstatement_id_seq_5700 start with 1;
alter sequence public.bankstatement_id_seq_5700 owner to postgres;

insert into users ( user_name, first_name, last_name, email,hash, phone, department, company, modelid, menu)
select  user_name, first_name, last_name, email,hash, phone, department, '5700', modelid, menu from users where user_name ='bate2' and company='5600';
insert into role (id, name, description, company,modelid) select id, name, description, '5700',modelid from role where company='5600';
insert into user_role ( userid, roleid, company, modelid) select  userid, roleid, '5700', modelid from user_role where company='5600';
insert into user_right (moduleid, roleid, short, company, modelid)  select   moduleid, roleid, short, '5700', modelid from user_right where company='5600';
insert into module (id, name, description, company, modelid, path, parent) select id, name, description, '5700',modelid, path, parent from module where company='5600';
insert into fmodule (id, name, description, company, modelid, account, is_debit, parent, acc_filter, oacc_filter) select id, name, description, '5700',modelid, account, is_debit, parent, acc_filter, oacc_filter from fmodule where company='5600';

insert into article (id,  name, description, parent, sprice, pprice, avg_price,currency, stocked, quantity_unit, pack_unit, account, oaccount, vat_code, company, modelid)
select id,  name, description, parent, sprice, pprice, avg_price,currency, stocked, quantity_unit, pack_unit, account, oaccount, vat_code, '5700', modelid from article where company='5600';


update module set parent='80' where id in ('10', '171', '172', '39', '38', '41') and company='5600';
update module set parent='30' where id in ('36', '37') and company='5600';
  insert into module (id, name, description,  company, modelid, path, parent)
  values ('1020', 'menu.favoris', 'Favoris', '5600', 400, '/', '-1' ),
          ('1334', 'menu.article', './form/ArticleForm', '5600', 400, '/art', '1020'),
          ('1315', 'menu.quantityUnit', './form/MasterfileForm', '5600', 400, '/qty', '1020'),
          ('1331', 'menu.supplier', './form/ArticleForm', '5600', 400, '/sup', '1020'),
          ('1333', 'menu.customer', './form/CustomerForm', '5600', 400, '/cust', '1020'),
          ('1335', 'menu.store', './form/CustomerForm', '5600', 400, '/store', '1020'),
          ('1339', 'menu.account', './form/AccountForm', '5600', 400, '/acc', '1020');

   insert into module (id, name, description,  company, modelid, path, parent)
   values ('1021', 'menu.sales', 'Ventes', '5600', 400, '/', '-1' ),
    ('13011', 'menu.transactions', './form/TransactionForm', '5600', 400, '/ltr', '1021');

    insert into module (id, name, description,  company, modelid, path, parent)
    values ('1022', 'menu.purchasing', 'Achats', '5600', 400, '/', '-1' ),
    ('13012', 'menu.transactions', './form/TransactionForm', '5600', 400, '/ltr', '1022');

    insert into module (id, name, description,  company, modelid, path, parent)
     values ('1023', 'menu.inventory', 'Gestion d stock', '5600', 400, '/', '-1' ),

    insert into fmodule (id, name, description,  account, is_debit, parent, company, modelid, copy_from, acc_filter, oacc_filter)
     values (136, 'menu.payroll', '/template/GoodreceivingTemplate1.docx', '', false, '30', '5600', 151, 136, '661', '421, 447' );

s   update fmodule set acc_filter='CL, 411, 52, 57, 58, 418', oacc_filter='701, 702, 705, 706, 707' where id='122' and company='5600';
    update fmodule set acc_filter='445,471, 60', oacc_filter='FS, 401, 408' where id='112' and company='5600';
    update fmodule set acc_filter='66', oacc_filter='421,422,447' where id='136' and company='5600';

    insert into fmodule (id, name, description,  account, is_debit, parent, company, modelid, copy_from, acc_filter, oacc_filter)
     values (118, 'Bank', 'Bank', '1', true, '1300', '5600', 151, 118, '52, FS, CL,701, 702, 705, 706, 707, 60, 66, 443, 445', '52, CL, FS' );

    insert into fmodule (id, name, description,  account, is_debit, parent, company, modelid, copy_from, acc_filter, oacc_filter)
            values (114, 'Immo', 'Immobilisation', '1', true, '1300', '5600', 151, 114, '2', 'FS, 401, 408' );

   update fmodule set acc_filter='52, FS, 701, 702, 705, 706, 707, 60, 66, 443, 445', oacc_filter='52, CL' where id=118 and company='5600';

     insert into module (id, name, description,  company, modelid, path, parent)
        values ('152', 'menu.room', 'Chambre', '5600', 400, '/room', '20' ),
         ('153', 'menu.apartment', 'Appartement', '5600', 400, '/apart', '20' ),
         ('154', 'menu.room', 'Batiment', '5600', 400, '/real', '20' );

         insert into module (id, name, description,  company, modelid, path, parent)
             values ('152', 'menu.room', 'Chambre', '1000', 400, '/room', '20' ),
             ('153', 'menu.apartment', 'Appartement', '1000', 400, '/apart', '20' ),
             ('154', 'menu.realEstate', 'Batiment', '1000', 400, '/real', '20' ),
             ('155', 'menu.floor', 'Etage', '1000', 400, '/floor', '20' );

         insert into user_right (moduleid,  roleid, short, company, modelid) values
                              ('152', 1, '+', 1000, 131), ('152', 1, 'r', 1000, 131), ('152', 1, 'w', 1000, 131),
                              ('152', 2, '+', 1000, 131),('152', 2, 'r', 1000, 131),('152', 2, 'w', 1000, 131),
                              ('153', 1, '+', 1000, 131), ('153', 1, 'r', 1000, 131), ('153', 1, 'w', 1000, 131),
                              ('153', 2, '+', 1000, 131),('153', 2, 'r', 1000, 131),('153', 2, 'w', 1000, 131),
                              ('154', 1, '+', 1000, 131), ('154', 1, 'r', 1000, 131), ('154', 1, 'w', 1000, 131),
                              ('154', 2, '+', 1000, 131),(154, 2, 'r', 1000, 131),(154, 2, 'w', 1000, 131)
                              ('155', 1, '+', 1000, 131), ('155', 1, 'r', 1000, 131), ('155', 1, 'w', 1000, 131),
                              ('155', 2, '+', 1000, 131),('155', 2, 'r', 1000, 131),('155', 2, 'w', 1000, 131);

create sequence master_compta_id_seq_1000 start with 5635;
alter sequence master_compta_id_seq_1000 owner to postgres;
create sequence public.details_compta_id_seq_1000 start with 7184;
alter sequence public.details_compta_id_seq_1000 owner to postgres;
create sequence public.transaction_id_seq_1000 start with 1375;
alter sequence public.transaction_id_seq_1000 owner to postgres;
create sequence public.transaction_details_id_seq_1000 start with 2726;
alter sequence public.transaction_details_id_seq_1000 owner to postgres;
create sequence public.transaction_log_id_seq_1000 start with 400;
alter sequence public.transaction_log_id_seq_1000 owner to postgres;
create sequence public.bankstatement_id_seq_1000 start with 43952;
alter sequence public.bankstatement_id_seq_1000 owner to postgres;

create sequence master_compta_id_seq_3000 start with 5635;
alter sequence master_compta_id_seq_3000 owner to postgres;
create sequence public.details_compta_id_seq_3000 start with 7184;
alter sequence public.details_compta_id_seq_3000 owner to postgres;
create sequence public.transaction_id_seq_3000 start with 1375;
alter sequence public.transaction_id_seq_3000 owner to postgres;
create sequence public.transaction_details_id_seq_3000 start with 2726;
alter sequence public.transaction_details_id_seq_3000 owner to postgres;
create sequence public.transaction_log_id_seq_3000 start with 400;
alter sequence public.transaction_log_id_seq_3000 owner to postgres;
create sequence public.bankstatement_id_seq_3000 start with 1;
alter sequence public.bankstatement_id_seq_3000 owner to postgres;

create sequence master_compta_id_seq_5000 start with 1;
alter sequence master_compta_id_seq_5000 owner to postgres;
create sequence public.details_compta_id_seq_5000 start with 1;
alter sequence public.details_compta_id_seq_5000 owner to postgres;
create sequence public.transaction_id_seq_5000 start with 1;
alter sequence public.transaction_id_seq_5000 owner to postgres;
create sequence public.transaction_details_id_seq_5000 start with 1;
alter sequence public.transaction_details_id_seq_5000 owner to postgres;
create sequence public.transaction_log_id_seq_5000 start with 1;
alter sequence public.transaction_log_id_seq_5000 owner to postgres;
create sequence public.journal_id_seq_5000 start with 1;
alter sequence public.journal_id_seq_5000 owner to postgres;
create sequence public.bankstatement_id_seq_5000 start with 1;
alter sequence public.bankstatement_id_seq_5000 owner to postgres;

create sequence master_compta_id_seq_5600 start with 1;
alter sequence master_compta_id_seq_5600 owner to postgres;
create sequence public.details_compta_id_seq_5600 start with 1;
alter sequence public.details_compta_id_seq_5600 owner to postgres;
create sequence public.transaction_id_seq_5600 start with 1;
alter sequence public.transaction_id_seq_5600 owner to postgres;
create sequence public.transaction_details_id_seq_5600 start with 1;
alter sequence public.transaction_details_id_seq_5600 owner to postgres;
create sequence public.transaction_log_id_seq_5600 start with 1;
alter sequence public.transaction_log_id_seq_5600 owner to postgres;
create sequence public.journal_id_seq_5600 start with 1;
alter sequence public.journal_id_seq_5600 owner to postgres;
create sequence public.bankstatement_id_seq_5600 start with 1;
alter sequence public.bankstatement_id_seq_5600 owner to postgres;

create sequence master_compta_id_seq_5700 start with 1;
alter sequence master_compta_id_seq_5700 owner to postgres;
create sequence public.details_compta_id_seq_5700 start with 1;
alter sequence public.details_compta_id_seq_5700 owner to postgres;
create sequence public.transaction_id_seq_5700 start with 1;
alter sequence public.transaction_id_seq_5700 owner to postgres;
create sequence public.transaction_details_id_seq_5700 start with 1;
alter sequence public.transaction_details_id_seq_5700 owner to postgres;
create sequence public.transaction_log_id_seq_5700 start with 1;
alter sequence public.transaction_log_id_seq_5700 owner to postgres;
create sequence public.journal_id_seq_5700 start with 1;
alter sequence public.journal_id_seq_5700 owner to postgres;
create sequence public.bankstatement_id_seq_5700 start with 1;
alter sequence public.bankstatement_id_seq_5700 owner to postgres;




select currval('transaction_log_id_seq');
select nextval('transaction_log_id_seq'::regclass);
select currval('transaction_id_seq');
select  nextval('transaction_id_seq'::regclass);
select currval('transaction_details_id_seq'::regclass);
select  nextval('transaction_details_id_seq'::regclass);

insert into article (id,  name, description, parent, sprice, pprice, avg_price,currency, stocked, quantity_unit, pack_unit, account, oaccount, vat_code, company, modelid)
    select id,  name, description, parent, sprice, pprice, avg_price,currency, stocked, quantity_unit, pack_unit, account, oaccount, vat_code, '5700', modelid from article where company='5600';


- prospects
un client potentiel:
- Saisie du prospect

2 - contact avec prospect:
   - voie/canal de communication: tel, mail, whatsUp,
   - decroché ou pas?
   - Resume: du contact sous form de text
   - Rendez-vous

 3  Rendez vous:
      - Date du rendez
      - Date de création du rendez
      - lieu
     - Object du rendez vous
     - Partnaire du rendez-vous prospect ou client
     - Resumee
     - Resultat:pas du rendez vous


    Tyoe   de contract
     - prestation
     - gardiennage
     - video surveillance
     -  Formation secourisme et prévention incendie
     - materiel indendie
     -




==

-