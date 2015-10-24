# dda-backup-crate


## [dda-backup-crate](https://github.com/DomainDrivenArchitecture/dda-backup-crate)
Der Crate stellt dem Nutzer die Installation & Konfiguration eines Anwendungs-Backup-Systems zur Verfügung. Applikationsspezifische Adapter können einfach erstellt werden.

## Kompatibilität
Der Crate funktioniert unter:
 * pallet 0.8
 * ubuntu 14.04

## Funktionalität
Der Backup-Crate leistet:
* Backup-User (dataBackupSource) auf dem Linux-Zielsystem angelegen. 
* Benötigte Ordnerstrukturen anlegen:
  * transport-outgoing - zur Ablage von Backups, die das laufende Backup-System auf eine andere Maschine bringt.
  * store - der Ort zur Ablage der momentan gespeicherten lokalen Backups.
  * restore - der Ort, von dem aus das Backup-System den Restore-Prozess anstößt.
* Backup-Scripte als Backup-Basissystem installieren.
* Benötigten Cronjobs eintragen.

 
## Features
* Der Crate bietet die Möglichkeit eines Backups von Files
   * Komprimierte (tar)
   * Directory Diffs
   * Datenbanken (mysql)
* und verwaltet diese Backups:
   * durch regelmäßige, automatisierte Datensicherungsvorgänge
   * durch eine Generationsverwaltung mit automatischer Löschung von zu alten Backups 
   * durch das (manuelle) Anstoßen eines Wiederherstellungsprozesses

## Methode install-backup-app-instance

* Die Methode **org.domaindrivenarchitecture.pallet.crate.backup/install-backup-app-instance** wird bei der Installation des Backups aufgerufen.
* Sie erhält folgende Eingabe-Parameter:

| Parameter       	| Bedeutung     |
| --------------- 	|-------------|
| app-name        	| Der Applikationssname (z.B. JIRA) |
| instance-name  	| Der semantische Name der Applikation |
| backup-lines   	| Kommandozeilen-Befehle zur Einrichtung des Backup-Prozesses  |
| source-transport-lines | Kommandozeilen-Befehle zur Einrichtung des Transport-Prozesses |
| restore-lines 	| Kommandozeilen-Befehle zur Einrichtung des Restore-Prozesses |
 
* Pro Applikation werden die drei Kommandozeilen-Befehls-Listen definiert.
* Es ist sinnvoll, diese Definitionen in einen eigenen Namensraum (Namespace) zu packen.
* Es ist sinnvoll, diese Definitionen parametrisierbar zu definieren, also sie z.B. mit einem Parameter für den Instanznamen zu versehen.
 
## Nutzungsbeispiel
### Genutzte Namespaces

```
(:require
	[org.domaindrivenarchitecture.pallet.crate.backup :as backup]
	[org.domaindrivenarchitecture.pallet.crate.backup.common-lib :as common-lib]
	[org.domaindrivenarchitecture.pallet.crate.backup.backup-lib :as backup-lib]
	[org.domaindrivenarchitecture.pallet.crate.backup.restore-lib :as restore-lib])
```

### Individueller Backup Adatpter 

```  
(defn owncloud-source-backup-script-lines
    ""
    [instance-name app-name mysql-pwd]]
    (into [] 
       (concat 
        common-lib/head
        common-lib/export-timestamp
        [(str "mv /home/dataBackupSource/store/"
       	    (common-lib/backup-file-prefix app-name instance-name :rsync)
             	"*."
              	(common-lib/file-type-extension :rsync)
               	" /home/dataBackupSource/transport-outgoing/"
               	(common-lib/backup-file-name app-name instance-name :rsync))
       	""]
        (common-lib/stop-app-server "apache2")         
       	(backup-lib/backup-mysql 
       	    :db-user "owncloud" 
            :db-pass mysql-pwd 
            :db-name "owncloud" 
            :app app-name
            :instance-name instance-name)
        (backup-lib/backup-files-rsync
            :root-dir "/var/www/" 
            :subdir-to-save "owncloud"
            :app app-name 
            :instance-name instance-name) 
        (common-lib/start-app-server "apache2"))))
```

### Individueller Transport Adapter

```
(defn owncloud-source-transport-script-lines
	[instance-name app-name generations]
  	(into [] 
       (concat 
          common-lib/head
          	(backup-lib/source-transport-script-lines 
            	:app-name app-name
            	:instance-name instance-name 
            	:gens-stored-on-source-system generations 
            	:files-to-transport [:rsync :mysql]))))
```

#### Individueller Restore Adapter

```
(defn owncloud-restore-script-lines
	[db-pass]
   		(into [] 
       		(concat 
          		common-lib/head
           		restore-lib/restore-parameters
           		restore-lib/restore-navigate-to-restore-location
           		(restore-lib/restore-locate-restore-dumps)
           		restore-lib/restore-head
           		(common-lib/prefix
           		" "
           		(common-lib/stop-app-server "apache2"))            
           		restore-lib/restore-db-head
           		(common-lib/prefix
       			"  "
       			(restore-lib/restore-mysql 
           			:db-user "owncloud" 
           			:db-pass db-pass 
           			:db-name "owncloud" ))
           		restore-lib/restore-db-tail
           		restore-lib/restore-file-head
           		(common-lib/prefix
       			"  " 
       			(restore-lib/restore-rsync
           			:restore-target-dir "/var/www/owncloud"))
            		restore-lib/restore-file-tail
            		restore-lib/restore-tail)))
```
  
### Installation

```  
(backup/install-backup-app-instance
           	:app-name app-name 
           	:instance-name instance-name
           	:backup-lines 
           	(owncloud-source-backup-script-lines
                instance-name app-name db-pass)
                :source-transport-lines 
           	(owncloud-source-transport-script-lines 
                instance-name app-name 1)
           	:restore-lines
           	(owncloud-restore-script-lines db-pass))))
```

## License

Author: Michael Jerger, Tobias Scherer, Thomas Jakob
Nuzung ist unter der Apache License, Version 2.0 (the "License").