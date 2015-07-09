# Umsetzung unter Verwendung des dda-backup-crate


## [dda-backup-crate](https://github.com/DomainDrivenArchitecture/dda-backup-crate)
Der Crate stellt dem Nutzer die Installation & Konfiguration eines applikationsspezifischen Backup-Systems bereit.

## Kompatibilität
Der Crate funktioniert unter:
 * pallet 0.8
 * ubuntu 14.04

## Funktionalität
Die Verwendung des Backup-Crates führt aus:
* benötigten Backup-User (dataBackupSource) auf dem Linux-Zielsystem angelegen. Backup-User verwaltet Ordner und Skripte, die für den Backup benutzt werden.
* benötigte Ordnerstrukturen anlegen, d.h. dass im home-Directory des neu angelegten Backup-Users folgende Ordner angelegt werden:
  * transport-outgoing - zur Ablage von Backups, die das laufende Backup-System auf eine andere Maschine bringt
  * store - der Ort zur Ablage der momentan gespeicherten lokalen Backups
  * restore - der Ort, von dem aus das Backup-Sysem den Restore-Prozess angestößt
* Backup-Scripte installieren; sie führen die benötigten Schritte zur Erstellung eines Backups aus. Je nach Applikation können diese unterschiedlich aussehen.
* die benötigten Cronjobs eintragen, welche für die regelmäßige Ausführung der oben genannten Scripte sorgen.

 
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


 
## Anwendungsbeispiele

### Skript-Definitionen am Beispiel von Owncloud
Es werden die drei Skripte definiert und anschließend die Installation der Backup-Architektur angestoßen.

#### Namespaces im Crate:
* backup: Hauptfunktionalität, z.B. Funktion zum Installationsaufruf, siehe unten
* commun-lib: Gemeinsam genutzte Funktionalitäten, z.B. Skript-Header, o.Ä.
* backup-lib: Funktionalitäten für den Backup-Schritt
* restore-lib: Funktionalitäten für den Restore-Schritt

```
(:require
	[org.domaindrivenarchitecture.pallet.crate.backup :as backup]
	[org.domaindrivenarchitecture.pallet.crate.backup.common-lib :as common-lib]
	[org.domaindrivenarchitecture.pallet.crate.backup.backup-lib :as backup-lib]
	[org.domaindrivenarchitecture.pallet.crate.backup.restore-lib :as restore-lib])
```

#### Skript zur Backup-Erstellung: 

* Sichern der Datenbank: backup-lib/backup-mysql
* Sichern der Files: backup-lib/backup-files-rsync

```  
(defn owncloud-source-backup-script-lines
    ""
    [& {:keys [semantic-name
       app-name
        mysql-pwd]}]
    (into [] 
       (concat 
        common-lib/head
        common-lib/export-timestamp
        [(str "mv /home/dataBackupSource/store/"
       	    (common-lib/backup-file-prefix app-name semantic-name :rsync)
             	"*."
              	(common-lib/file-type-extension :rsync)
               	" /home/dataBackupSource/transport-outgoing/"
               	(common-lib/backup-file-name app-name semantic-name :rsync))
       	""]
        (common-lib/stop-app-server "apache2")         
       	(backup-lib/backup-mysql 
       	    :db-user "owncloud" 
            :db-pass mysql-pwd 
            :db-name "owncloud" 
            :app app-name
            :semantic-name semantic-name)
        (backup-lib/backup-files-rsync
            :root-dir "/var/www/" 
            :subdir-to-save "owncloud"
            :app app-name 
            :semantic-name semantic-name) 
        (common-lib/start-app-server "apache2"))))
```

#### Skript für den Transport:
```
(defn owncloud-source-transport-script-lines
	[& {:keys [semantic-name
       		app-name
             	generations]}]
  	(into [] 
        	(concat 
          		common-lib/head
          		(backup-lib/source-transport-script-lines 
            		:app-name app-name
            		:semantic-name semantic-name 
            		:gens-stored-on-source-system generations 
            		:files-to-transport [:rsync :mysql]))))
```

#### Skript für die Wiederherstellung:
```
(defn owncloud-restore-script-lines
	[& {:keys [db-pass]}]
  		(let 	[db-user "owncloud"
        		db-name "owncloud"]
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
                			:db-user db-user 
                			:db-pass db-pass 
                			:db-name db-name))
            		restore-lib/restore-db-tail
            		restore-lib/restore-file-head
            		(common-lib/prefix
              			"  " 
              			(restore-lib/restore-rsync
                			:restore-target-dir "/var/www/owncloud"))
            		restore-lib/restore-file-tail
            		restore-lib/restore-tail))))
```
  
#### Installationsaufruf:

```  
(backup/install-backup-app-instance
           	:app-name app-name 
           	:instance-name semantic-name
           	:backup-lines 
           	(owncloud-source-backup-script-lines
                :semantic-name semantic-name
                :app-name app-name
                :mysql-pwd db-pass)
                :source-transport-lines 
           	(owncloud-source-transport-script-lines 
                :semantic-name semantic-name
                :app-name app-name
                :generations 1)
           	:restore-lines
           	(owncloud-restore-script-lines 
             	:db-pass db-pass))))
```
  
  

## License

Copyright © 2015, Michael Jerger, Tobias Scherer

Distributed under the Apache 2.0 License.
