# dda-backup-crate


## [dda-backup-crate](https://github.com/DomainDrivenArchitecture/dda-backup-crate)
The backup crate provides installation & configuration application backups. Specific adapters has to be defined.

## Compatibility
The Crate works with:
 * pallet 0.8
 * ubuntu 14.04

## Funktionality
The backup Crate provides:
* Backup User ( dataBackupSource ) is put on the target Linux system.
* Creating required folder structures :
  * **transport** -outgoing - to store backups that brings the current backup system to another machine.
  * **store** - the place to store the currently stored local backups.
  * **restore** - the place from which the backup system initiates the restore process.
* Install backup scripts as backup base system.
* Enter required cronjobs.

## Usageexample

### Namespaces used
```
(:require
	[org.domaindrivenarchitecture.pallet.crate.backup :as backup]
	[org.domaindrivenarchitecture.pallet.crate.backup.common-lib :as common-lib]
	[org.domaindrivenarchitecture.pallet.crate.backup.backup-lib :as backup-lib]
	[org.domaindrivenarchitecture.pallet.crate.backup.restore-lib :as restore-lib])
```

### Individual Backup Adatpter 
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

### Individual Transport Adapter
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

#### Individual Restore Adapter
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
Licensed under the Apache License, Version 2.0 (the "License");