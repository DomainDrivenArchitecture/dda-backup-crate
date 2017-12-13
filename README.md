# dda-backup-crate

[![Clojars Project](https://img.shields.io/clojars/v/dda/dda-backup-crate.svg)](https://clojars.org/dda/dda-backup-crate)
[![Build Status](https://travis-ci.org/DomainDrivenArchitecture/dda-backup-crate.svg?branch=master)](https://travis-ci.org/DomainDrivenArchitecture/dda-backup-crate)

[![Slack](https://img.shields.io/badge/chat-clojurians-green.svg?style=flat)](https://clojurians.slack.com/messages/#dda-pallet/) | [<img src="https://domaindrivenarchitecture.org/img/meetup.svg" width=50 alt="DevOps Hacking with Clojure Meetup"> DevOps Hacking with Clojure](https://www.meetup.com/de-DE/preview/dda-pallet-DevOps-Hacking-with-Clojure) | [Website & Blog](https://domaindrivenarchitecture.org)

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
* manages the required cronjobs.

## Usageexample

### Namespaces used
```
(:require
	[org.domaindrivenarchitecture.pallet.crate.backup :as backup])
```

### Individual Backup Configuration
```  
(def my-config
  {:backup-name "managed-vm"
   :script-path "/usr/lib/dda-backup/"
   :gens-stored-on-source-system 1
   :elements [{:type :file-compressed
               :name "user-home"
               :root-dir "/home/some-user"
               :subdir-to-save ".ssh .mozilla"}]
   :backup-user {:name "dataBackupSource"
                 :encrypted-passwd "someEncryptedPwd"}}
```

Backs up .ssh and .mozilla in /home/some-user.

### Installation

```  
(backup/install "app-name" my-config)
```

### Configuraton

```  
(backup/configure "app-name" my-config)
```

### Do the restore
```
cd /home/dataBackupSource/restore/
/usr/lib/dda-backup/app-name_restore.sh [prefix for restore files]
```

## Using Duplicity
Concerning duplicity-ppa, in the case of "Package source : FAIL"
the second try will work.

## License

Author: Michael Jerger, Tobias Scherer, Thomas Jakob
Licensed under the Apache License, Version 2.0 (the "License");
