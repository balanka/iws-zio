update  fmodule set id=102 where id=111;
insert into fmodule (id, name, description, account, is_debit, company, modelid, parent,copy_from,acc_filter,oacc_filter ) VALUES
  (100, 'R.Q.F'               , 'Request 4 quotation', '', false, '1000', 151, '1301','','' ,''),
  (100, 'Appel d''offre client', 'Appel d''offre client', '', false, '5000', 151, '1301','','' ,''),
  (100, 'Appel d''offre client', 'Appel d''offre client', '', false, '5500', 151, '1301','','' ,''),
  (100, 'Appel d''offre client', 'Appel d''offre client', '', false, '5600', 151, '1301','','' ,''),
  (100, 'Appel d''offre client', 'Appel d''offre client', '', false, '5700', 151, '1301','','' ,''),

  (101, 'P. Requisition', 'P. Requisition', '', false, '1000', 151, '1301','','' ,''),
  (101, 'Demande d''appro', 'Demande d''appro', '', false, '5000', 151, '1301','','' ,''),
  (101, 'Demande d''appro', 'Demande d''appro', '', false, '5500', 151, '1301','','' ,''),
  (101, 'Demande d''appro', 'Demande d''appro', '', false, '5600', 151, '1301','','' ,''),
  (101, 'Demande d''appro', 'Demande d''appro', '', false, '5700', 151, '1301','','' ,''),

  (103, 'P. contract', 'P. contract', '', false, '1000', 151, '1301','','' ,''),
  (103, 'Contrat d''appro', 'Contrat d''appro', '', false, '5000', 151, '1301','','' ,''),
  (103, 'Contrat d''appro', 'Contrat d''appro', '', false, '5500', 151, '1301','','' ,''),
  (103, 'Contrat d''appro', 'Contrat d''appro', '', false, '5600', 151, '1301','','' ,''),
  (103, 'Contrat d''appro', 'Contrat d''appro', '', false, '5700', 151, '1301','','' ,''),

  (126, 'Stock transfer', 'Stock transfer', '', false, '1000', 151, '1301','','' ,''),
  (126, 'Transfer de stock', 'Transfer de stock', '', false, '5000', 151, '1301','','' ,''),
  (126, 'Transfer de stock', 'Transfer de stock', '', false, '5500', 151, '1301','','' ,''),
  (126, 'Transfer de stock', 'Transfer de stock', '', false, '5600', 151, '1301','','' ,''),
  (126, 'Transfer de stock', 'Transfer de stock', '', false, '5700', 151, '1301','','' ,''),

  (127, 'Consumption', 'Consumption', '', false, '1000', 151, '1301','','' ,''),
  (127, 'Consommation', 'Consommation', '', false, '5000', 151, '1301','','' ,''),
  (127, 'Consommation', 'Consommation', '', false, '5500', 151, '1301','','' ,''),
  (127, 'Consommation', 'Consommation', '', false, '5600', 151, '1301','','' ,''),
  (127, 'Consommation', 'Consommation', '', false, '5700', 151, '1301','','' ,''),

  (128, 'Stocktake', 'Stocktake', '', false, '1000', 151, '1301','','' ,''),
  (128, 'Inventaire', 'Inventaire', '', false, '5000', 151, '1301','','' ,''),
  (128, 'Inventaire', 'Inventaire', '', false, '5500', 151, '1301','','' ,''),
  (128, 'Inventaire', 'Inventaire', '', false, '5600', 151, '1301','','' ,''),
  (128, 'Inventaire', 'Inventaire', '', false, '5700', 151, '1301','','' ,'');


insert into module (id,name,description,company,modelid,path,parent)
select id,name,description,'5600',modelid,path,parent from module where id='173' and company='1000';
insert into user_right (moduleid,roleid,short,company,modelid ) select moduleid,roleid,short,'5600',modelid from user_right where  moduleid ='173' and company='5700';

insert into partner (id, name,description,street,zip,city,state,country ,phone,email, company,modelid ) select id, name,description,street,zip,city,state,country ,phone,email, '5700' ,modelid  from partner where id='4711';
update partner set name='Touré Asmiou' where id='001';
update partner set id='001' where id='4711' and company='5700';

update periodic_account_balance set account =TRIM(account), id= TRIM(id);
update details_compta set account =TRIM (account), oaccount=TRIM(oaccount);
update journal set account =TRIM(account), oaccount=TRIM(oaccount);
update master_compta  set account = TRIM (account) where LENGTH(TRIM(account))>0;

insert into user_right (moduleid, roleid, short, company,modelid)
select moduleid, roleid, short, '5700',modelid from user_right
where company='1000' and moduleid=173;

insert into module (id, name, description, company, modelid, path, parent)
select id, name, description, '5700', modelid, path, parent from module
                      where  company='1000' and id='173';



alter table customer add column contact varchar;
alter table customer alter column contact SET DEFAULT '';
update customer set contact ='' where contact is null;
alter table supplier add column contact varchar;
alter table supplier alter column contact SET DEFAULT '';
update supplier set contact ='' where contact is null;
alter table employee add column contact varchar;
alter table employee alter column contact SET DEFAULT '';
update employee set contact ='' where contact is null;
alter table company rename column partner to contact;
alter table company alter column contact SET DEFAULT '';
update company set contact ='' where contact is null;

alter table company rename column partner to contact;
alter table company alter column contact SET DEFAULT '';
alter table master_compta rename column id1 to contact;
alter table master_compta alter column contact TYPE varchar;
alter table master_compta alter column contact SET DEFAULT '';
alter table master_compta alter column oid TYPE varchar;
alter table master_compta alter column oid SET DEFAULT '';
alter table master_compta rename column type_journal to foot_text;
alter table master_compta alter column foot_text TYPE varchar;
alter table master_compta alter column foot_text SET DEFAULT '';

alter table transaction rename column id1 to contact;
alter table transaction alter column contact TYPE varchar;
alter table transaction alter column contact SET DEFAULT '';
alter table transaction alter column oid TYPE varchar;
alter table transaction alter column oid SET DEFAULT '';
alter table transaction rename column type_journal to foot_text;
alter table transaction alter column foot_text TYPE varchar;
alter table transaction alter column foot_text SET DEFAULT '';

alter table transaction_log rename column id1 to contact;
alter table transaction_log alter column contact TYPE varchar;
alter table transaction_log alter column contact SET DEFAULT '';
alter table transaction_log alter column oid TYPE varchar;
alter table transaction_log add column foot_text varchar default '';

alter table journal alter column oid TYPE varchar;
alter table journal alter column oid SET DEFAULT '';



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

7299,118
7145,-1
6773,122
6098,1300
6725,136
7119,124
7184,114
7117,134
6057,144
7185,112
create sequence public.transaction_id_seq_5000_100 start with 1;
alter sequence public.transaction_id_seq_5000_100 owner to postgres;
create sequence public.transaction_id_seq_5000_102 start with 1;
alter sequence public.transaction_id_seq_5000_102 owner to postgres;
create sequence public.transaction_id_seq_5000_103 start with 1;
alter sequence public.transaction_id_seq_5000_103 owner to postgres;
create sequence public.transaction_id_seq_5000_126 start with 1;
alter sequence public.transaction_id_seq_5000_126 owner to postgres;
create sequence public.transaction_id_seq_5000_127 start with 1;
alter sequence public.transaction_id_seq_5000_127 owner to postgres;
create sequence public.transaction_id_seq_5000_128 start with 1;
alter sequence public.transaction_id_seq_5000_128 owner to postgres;

create sequence public.transaction_id_seq_5500_100 start with 1;
alter sequence public.transaction_id_seq_5500_100 owner to postgres;
create sequence public.transaction_id_seq_5500_102 start with 1;
alter sequence public.transaction_id_seq_5500_102 owner to postgres;
create sequence public.transaction_id_seq_5500_103 start with 1;
alter sequence public.transaction_id_seq_5500_103 owner to postgres;
create sequence public.transaction_id_seq_5500_126 start with 1;
alter sequence public.transaction_id_seq_5500_126 owner to postgres;
create sequence public.transaction_id_seq_5500_127 start with 1;
alter sequence public.transaction_id_seq_5500_127 owner to postgres;
create sequence public.transaction_id_seq_5500_128 start with 1;
alter sequence public.transaction_id_seq_5500_128 owner to postgres;

create sequence public.transaction_id_seq_5600_100 start with 1;
alter sequence public.transaction_id_seq_5600_100 owner to postgres;
create sequence public.transaction_id_seq_5600_101 start with 1;
alter sequence public.transaction_id_seq_5600_101 owner to postgres;
create sequence public.transaction_id_seq_5600_102 start with 1;
alter sequence public.transaction_id_seq_5600_102 owner to postgres;
create sequence public.transaction_id_seq_5600_103 start with 1;
alter sequence public.transaction_id_seq_5600_103 owner to postgres;
create sequence public.transaction_id_seq_5600_126 start with 1;
alter sequence public.transaction_id_seq_5600_126 owner to postgres;
create sequence public.transaction_id_seq_5600_127 start with 1;
alter sequence public.transaction_id_seq_5600_127 owner to postgres;
create sequence public.transaction_id_seq_5600_128 start with 1;
alter sequence public.transaction_id_seq_5600_128 owner to postgres;

create sequence public.transaction_id_seq_5700_100 start with 1;
alter sequence public.transaction_id_seq_5700_100 owner to postgres;
create sequence public.transaction_id_seq_5700_102 start with 1;
alter sequence public.transaction_id_seq_5700_102 owner to postgres;
create sequence public.transaction_id_seq_5700_103 start with 1;
alter sequence public.transaction_id_seq_5700_103 owner to postgres;
create sequence public.transaction_id_seq_5700_126 start with 1;
alter sequence public.transaction_id_seq_5700_126 owner to postgres;
create sequence public.transaction_id_seq_5700_127 start with 1;
alter sequence public.transaction_id_seq_5700_127 owner to postgres;
create sequence public.transaction_id_seq_5700_128 start with 1;
alter sequence public.transaction_id_seq_5700_128 owner to postgres;

create sequence master_compta_id_seq_5700_104 start with 1;
alter sequence master_compta_id_seq_5700_104 owner to postgres;
create sequence public.transaction_id_seq_5700_104 start with 1;
alter sequence public.transaction_id_seq_5700_104 owner to postgres;

create sequence master_compta_id_seq_5700_105 start with 1;
alter sequence master_compta_id_seq_5700_105 owner to postgres;
create sequence public.transaction_id_seq_5700_105 start with 1;
alter sequence public.transaction_id_seq_5700_105 owner to postgres;

create sequence master_compta_id_seq_5700_109 start with 1;
alter sequence master_compta_id_seq_5700_109 owner to postgres;
create sequence public.transaction_id_seq_5700_109 start with 1;
alter sequence public.transaction_id_seq_5700_109 owner to postgres;

create sequence master_compta_id_seq_5700_110 start with 1;
alter sequence master_compta_id_seq_5700_110 owner to postgres;
create sequence public.transaction_id_seq_5700_110 start with 1;
alter sequence public.transaction_id_seq_5700_110 owner to postgres;

create sequence master_compta_id_seq_5700_111 start with 1;
alter sequence master_compta_id_seq_5700_111 owner to postgres;
create sequence public.transaction_id_seq_5700_111 start with 1;
alter sequence public.transaction_id_seq_5700_111 owner to postgres;

create sequence master_compta_id_seq_5700_112 start with 1;
alter sequence master_compta_id_seq_5700_112 owner to postgres;
create sequence public.transaction_id_seq_5700_112 start with 1;
alter sequence public.transaction_id_seq_5700_112 owner to postgres;

create sequence master_compta_id_seq_5700_114 start with 1;
alter sequence master_compta_id_seq_5700_114 owner to postgres;
create sequence public.transaction_id_seq_5700_114 start with 1;
alter sequence public.transaction_id_seq_5700_114 owner to postgres;

create sequence master_compta_id_seq_5700_118 start with 1;
alter sequence master_compta_id_seq_5700_118 owner to postgres;
create sequence public.transaction_id_seq_5700_118 start with 1;
alter sequence public.transaction_id_seq_5700_118 owner to postgres;

create sequence master_compta_id_seq_5700_122 start with 23;
alter sequence master_compta_id_seq_5700_122 owner to postgres;
create sequence public.transaction_id_seq_5700_122 start with 1;
alter sequence public.transaction_id_seq_5700_122 owner to postgres;

create sequence master_compta_id_seq_5700_124 start with 1;
alter sequence master_compta_id_seq_5700_124 owner to postgres;
create sequence public.transaction_id_seq_5700_124 start with 1;
alter sequence public.transaction_id_seq_5700_124 owner to postgres;

create sequence master_compta_id_seq_5700_134 start with 1;
alter sequence master_compta_id_seq_5700_134 owner to postgres;
create sequence public.transaction_id_seq_5700_134 start with 1;
alter sequence public.transaction_id_seq_5700_134 owner to postgres;

create sequence master_compta_id_seq_5700_136 start with 1;
alter sequence master_compta_id_seq_5700_136 owner to postgres;
create sequence public.transaction_id_seq_5700_136 start with 1;
alter sequence public.transaction_id_seq_5700_136 owner to postgres;

create sequence master_compta_id_seq_5700_144 start with 1;
alter sequence master_compta_id_seq_5700_144 owner to postgres;
create sequence public.transaction_id_seq_5700_144 start with 1;
alter sequence public.transaction_id_seq_5700_144 owner to postgres;

create sequence master_compta_id_seq_5700_1006 start with 1;
alter sequence master_compta_id_seq_5700_1006 owner to postgres;
create sequence public.transaction_id_seq_5700_1006 start with 1;
alter sequence public.transaction_id_seq_5700_1006 owner to postgres;

create sequence master_compta_id_seq_5700_1017 start with 1;
alter sequence master_compta_id_seq_5700_1017 owner to postgres;
create sequence public.transaction_id_seq_5700_1017 start with 1;
alter sequence public.transaction_id_seq_5700_1017 owner to postgres;

create sequence master_compta_id_seq_5700_1018 start with 1;
alter sequence master_compta_id_seq_5700_1018 owner to postgres;
create sequence public.transaction_id_seq_5700_1018 start with 1;
alter sequence public.transaction_id_seq_5700_1018 owner to postgres;


create sequence master_compta_id_seq_5700_1019 start with 1;
alter sequence master_compta_id_seq_5700_1019 owner to postgres;
create sequence public.transaction_id_seq_5700_1019 start with 1;
alter sequence public.transaction_id_seq_5700_1019 owner to postgres;


create sequence master_compta_id_seq_5600_104 start with 1;
alter sequence master_compta_id_seq_5600_104 owner to postgres;
create sequence public.transaction_id_seq_5600_104 start with 1;
alter sequence public.transaction_id_seq_5600_104 owner to postgres;

ALTER SEQUENCE transaction_id_seq_5700_104 START WITH 1;
--ALTER SEQUENCE transaction_id_seq_5700_105 START WITH 1;

create sequence master_compta_id_seq_5600_105 start with 1;
alter sequence master_compta_id_seq_5600_105 owner to postgres;
create sequence public.transaction_id_seq_5600_105 start with 1;
alter sequence public.transaction_id_seq_5600_105 owner to postgres;

create sequence master_compta_id_seq_5600_109 start with 1;
alter sequence master_compta_id_seq_5600_109 owner to postgres;
create sequence public.transaction_id_seq_5600_109 start with 1;
alter sequence public.transaction_id_seq_5600_109 owner to postgres;

create sequence master_compta_id_seq_5600_110 start with 1;
alter sequence master_compta_id_seq_5600_110 owner to postgres;
create sequence public.transaction_id_seq_5600_110 start with 1;
alter sequence public.transaction_id_seq_5600_110 owner to postgres;

create sequence master_compta_id_seq_5600_111 start with 1;
alter sequence master_compta_id_seq_5600_111 owner to postgres;
create sequence public.transaction_id_seq_5600_111 start with 1;
alter sequence public.transaction_id_seq_5600_111 owner to postgres;

create sequence master_compta_id_seq_5600_112 start with 1;
alter sequence master_compta_id_seq_5600_112 owner to postgres;
create sequence public.transaction_id_seq_5600_112 start with 1;
alter sequence public.transaction_id_seq_5600_112 owner to postgres;

create sequence master_compta_id_seq_5600_114 start with 7185;
alter sequence master_compta_id_seq_5600_114 owner to postgres;
create sequence public.transaction_id_seq_5600_114 start with 1375;
alter sequence public.transaction_id_seq_5600_114 owner to postgres;

create sequence master_compta_id_seq_5600_118 start with 7300;
alter sequence master_compta_id_seq_5600_118 owner to postgres;
create sequence public.transaction_id_seq_5600_118 start with 1375;
alter sequence public.transaction_id_seq_5600_118 owner to postgres;

create sequence master_compta_id_seq_5600_122 start with 6774;
alter sequence master_compta_id_seq_5600_122 owner to postgres;
create sequence public.transaction_id_seq_5600_122 start with 1375;
alter sequence public.transaction_id_seq_5600_122 owner to postgres;

create sequence master_compta_id_seq_5600_124 start with 7120;
alter sequence master_compta_id_seq_5600_124 owner to postgres;
create sequence public.transaction_id_seq_5600_124 start with 1375;
alter sequence public.transaction_id_seq_5600_124 owner to postgres;

create sequence master_compta_id_seq_5600_134 start with 7118;
alter sequence master_compta_id_seq_5600_134 owner to postgres;
create sequence public.transaction_id_seq_5600_134 start with 1375;
alter sequence public.transaction_id_seq_5600_134 owner to postgres;

create sequence master_compta_id_seq_5600_136 start with 6726;
alter sequence master_compta_id_seq_5600_136 owner to postgres;
create sequence public.transaction_id_seq_5600_136 start with 1375;
alter sequence public.transaction_id_seq_5600_136 owner to postgres;

create sequence master_compta_id_seq_5600_144 start with 1;
alter sequence master_compta_id_seq_5600_144 owner to postgres;
create sequence public.transaction_id_seq_5600_144 start with 1;
alter sequence public.transaction_id_seq_5600_144 owner to postgres;

create sequence master_compta_id_seq_5600_1006 start with 1;
alter sequence master_compta_id_seq_5600_1006 owner to postgres;
create sequence public.transaction_id_seq_5600_1006 start with 1;
alter sequence public.transaction_id_seq_5600_1006 owner to postgres;

create sequence master_compta_id_seq_5600_1017 start with 1;
alter sequence master_compta_id_seq_5600_1017 owner to postgres;
create sequence public.transaction_id_seq_5600_1017 start with 1;
alter sequence public.transaction_id_seq_5600_1017 owner to postgres;

create sequence master_compta_id_seq_5600_1018 start with 1;
alter sequence master_compta_id_seq_5600_1018 owner to postgres;
create sequence public.transaction_id_seq_5600_1018 start with 1;
alter sequence public.transaction_id_seq_5600_1018 owner to postgres;


create sequence master_compta_id_seq_5600_1019 start with 1;
alter sequence master_compta_id_seq_5600_1019 owner to postgres;
create sequence public.transaction_id_seq_5600_1019 start with 1;
alter sequence public.transaction_id_seq_5600_1019 owner to postgres;

select currval('transaction_log_id_seq');
select nextval('transaction_log_id_seq'::regclass);
select currval('transaction_id_seq');
select  nextval('transaction_id_seq'::regclass);
select currval('transaction_details_id_seq'::regclass);
select  nextval('transaction_details_id_seq'::regclass);


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