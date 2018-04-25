# dda-backup-crate

[![Clojars Project](https://img.shields.io/clojars/v/dda/dda-backup-crate.svg)](https://clojars.org/dda/dda-backup-crate)
[![Build Status](https://travis-ci.org/DomainDrivenArchitecture/dda-backup-crate.svg?branch=master)](https://travis-ci.org/DomainDrivenArchitecture/dda-backup-crate)

[![Slack](https://img.shields.io/badge/chat-clojurians-green.svg?style=flat)](https://clojurians.slack.com/messages/#dda-pallet/) | [<img src="https://domaindrivenarchitecture.org/img/meetup.svg" width=50 alt="DevOps Hacking with Clojure Meetup"> DevOps Hacking with Clojure](https://www.meetup.com/de-DE/preview/dda-pallet-DevOps-Hacking-with-Clojure) | [Website & Blog](https://domaindrivenarchitecture.org)

## Jump to
[Usage](#usage)
[Additional-info-about-the-configuration](#additional-info-about-the-configuration)
[Targets-config-example](#targets-config-example)
[Backup-config-example](#backup-config-example)
[Reference-Targets](#targets)
[Reference-Domain-API](#domain-api)
[Reference-Infra-API](#infra-api)
[Compatibility](#compatibility)
[License](#license)

## Features
The backup Crate provides:
* Backup User ( dataBackupSource ) is put on the target Linux system.
* Creating required folder structures :
  * **transport** -outgoing - to store backups that brings the current backup system to another machine.
  * **store** - the place to store the currently stored local backups.
  * **restore** - the place from which the backup system initiates the restore process.
* Install backup scripts as backup base system.
* manages the required cronjobs.

## Usage
1. **Download the jar-file** from the releases page of this repository (e.g. `curl -L -o backup.jar https://github.com/DomainDrivenArchitecture/dda-backup-crate/releases/download/1.0.1/dda-backup-crate-1.0.1-standalone.jar`)
1. **Create the ```backup.edn``` configruration** file in the same folder where you saved the jar-file. The ```backup.edn``` file specifies the configuration used to generate the scripts for
backing up and restoring the system. You may use the following example as a starting point and adjust it according to your own needs:

```clojure
{:backup-name          "downloads"                                                       ;name of the backup to be created
 :backup-user          {:clear-password {:plain "test1234"}}                             ;password for the backup-user
 :local-management     {:gens-stored-on-source-system 3}                                 ;the number of backup generations to be saved before deleting backups
 :transport-management {}
 :backup-elements      [{:type        :file-compressed                                   ;type of the backup
                         :name        "downloads_file_compressed"                        ;name of the backup-element
                         :backup-path ["/home/krj/Downloads/" "/home/krj/Documents/"]}   ;the paths which are supposed to be backed up and restored
                        {:type        :rsync                                             ;use rsync for backup on the same host
                         :name        "downloads_rsync"                                  ;name of the backup-element
                         :backup-path ["/home/krj/Downloads/" "/home/krj/Documents/"]}]  ;the paths which are supposed to be backed up and restored 
 }
  ```
  
3. The target hosts need to be specified in the `integration/resources/existing-targets.edn`. In this file you define which servers are suppose to receive the backup and restore scripts. You may use and adjust the following example config:
```clojure
{:existing [{:node-name "target1"                      ; semantic name (keep the default or use a name that suits you)
             :node-ip "192.168.56.104"}]               ; the ip4 address of the machine to be provisioned
             {:node-name "target2"                     ; semantic name (keep the default or use a name that suits you)
                          :node-ip "192.168.56.105"}]  ; the ip4 address of the machine to be provisioned
 :provisioning-user {:login "initial"                  ; user on the target machine, must have sudo rights
                     :password {:plain "secure1234"}}} ; password can be ommited, if a ssh key is authorized
```

5. **Run the jar** with the following options and inspect the output.
  For testing against localhost:
  ```bash
java -jar dda-backup-crate-standalone.jar backup.edn
  ```

  For testing remote server(s) please specify the targets file:

  ```bash
java -jar dda-backup-crate-standalone.jar --targets targets.edn backup.edn
```

## Additional-info-about-the-configuration
Two configuration files are required by the dda-backup-crate:: "backup.edn" and "targets.edn" (or similar names). These files specify both WHAT to backup and restore and WHERE. In detail: the first file defines the configuration for the backup and restore scripts, while the second configuration file specifies the target nodes/systems, on which the installation and configurationg will be performed. The following examples will explain these files more in details.

(**Remark:** The second file "targets.edn" is *optional*. This means, if none is specified, then a default file is used, which defines that the installation and configuration are performed against  **localhost**.)

### Targets-config-example
```clojure
{:existing [{:node-name "test-vm1"
             :node-ip "35.157.19.218"}
            {:node-name "test-vm2"
             :node-ip "18.194.113.138"}]
 :provisioning-user {:login "ubuntu"}}
```
The keyword ```:existing``` has to be assigned a vector, that contains maps with the information about the nodes.
The nodes are the target machines that will be tested. The ```node-name``` has to be set to be able to identify the target machine and the ```node-ip``` has to be set so that the source machine can reach it.
The ```provisioning-user``` has to be the same for all nodes that will be tested. Furthermore, if the ssh-key of the executing host is authorized on all target nodes, a password for authorization can be omitted. If this is not the case, the provisioning user has to contain a password.

### Backup-config-example
```clojure
{:backup-name          "downloads"                                                    
 :backup-user          {:clear-password {:plain "test1234"}}                          
 :local-management     {:gens-stored-on-source-system 3}                          
 :transport-management {}
 :backup-elements      [{:type        :file-compressed                              
                         :name        "downloads_file_compressed"                        
                         :backup-path ["/home/krj/Downloads/" "/home/krj/Documents/"]}   
                        {:type        :rsync                                            
                         :name        "downloads_rsync"                                
                         :backup-path ["/home/krj/Downloads/" "/home/krj/Documents/"]}]
```
The backup config file determines the resulting backup and restore script files. For example the part containing ```{:type :file-compressed}``` creates a backup script that uses tar and compressed the files to be backed-up.
There are different types of backups that can be used. More details can be found in the reference below.

## Reference
You will find here the reference for
* target: How targets can be specified
* Domain-Level-API: The high level API with many built-in conventions.
* Infra-Level-API: If the domain conventions don't fit your needs, you can use our low-level API (infra) and easily realize your own conventions.

### Targets
The schema of the domain layer for the targets is:
```clojure
(def ExistingNode
  "Represents a target node with ip and its name."
  {:node-name s/Str   ; semantic name (keep the default or use a name that suits you)
   :node-ip s/Str})   ; the ip4 address of the machine to be provisioned

(def ExistingNodes
  "A sequence of ExistingNodes."
  {s/Keyword [ExistingNode]})

(def ProvisioningUser
  "User used for provisioning."
  {:login s/Str                                ; user on the target machine, must have sudo rights
   (s/optional-key :password) secret/Secret})  ; password can be ommited, if a ssh key is authorized

(def Targets
  "Targets to be used during provisioning."
  {:existing [ExistingNode]                                ; one ore more target nodes.
   (s/optional-key :provisioning-user) ProvisioningUser})  ; user can be ommited to execute on localhost with current user
```
The "targets.edn" file has to match this schema.

### Domain-API
The schema for the tests is:
```clojure
(def BackupElementType
  "The backup source elements"
  (s/enum :mysql :file-compressed :file-plain :rsync))

(def BackupElementType element-type/BackupElementType)

(def TransportType
  (s/enum :ssh-pull :duplicity-push))

(def BackupBaseElement
  {:type BackupElementType
   :name s/Str})

(def BackupDbElement
  "The db backup elements"
  {:db-user-name s/Str
   :db-user-passwd secret/Secret
   :db-name s/Str
   (s/optional-key :db-create-options) s/Str
   (s/optional-key :db-pre-processing) [s/Str]
   (s/optional-key :db-post-processing) [s/Str]})

(def BackupPath
    {:backup-path [directory-model/NonRootDirectory]
     (s/optional-key :new-owner) s/Str})

(def BackupElement
  "The backup elements"
  (s/conditional
   #(= (:type %) :mysql)
   (merge
    BackupBaseElement
    BackupDbElement)
   #(= (:type %) :file-compressed)
   (merge
    BackupBaseElement
    BackupPath)
   #(= (:type %) :file-plain)
   (merge
     BackupBaseElement
     BackupPath)
   #(= (:type %) :rsync)
   (merge
     BackupBaseElement
     BackupPath)))

(def LocalManagement
  {:gens-stored-on-source-system s/Num})

(def TransportManagement
  {(s/optional-key :ssh-pull) s/Any
   (s/optional-key :duplicity-push)
   {:public-key secret/Secret
    :private-key secret/Secret
    :passphrase secret/Secret
    (s/optional-key :target-s3) {:aws-access-key-id secret/Secret
                                 :aws-secret-access-key secret/Secret
                                 :bucket-name s/Str
                                 (s/optional-key :directory-name) s/Str}}})

(def BackupConfig
  {:backup-name s/Str
   :backup-user user/User
   (s/optional-key :service-restart) s/Str
   :local-management LocalManagement
   :transport-management TransportManagement
   :backup-elements [BackupElement]})
```
The "backup.edn" file has to match this schema.

### Infra-API
The infra configuration is a configuration on the infrastructure level of a crate. It contains the complete configuration options that are possible with the crate functions.

The schema is:
```clojure
(def BackupElementType
  "The backup source elements"
  (s/enum :mysql :file-compressed :file-plain :rsync))

(def TransportType
  (s/enum :ssh-pull :duplicity-push))

(def ScriptType
  "The backup elements"
  (s/enum :backup :restore :source-transport))

(def BackupBaseElement
  {:type BackupElementType
   :name s/Str
   :backup-file-name s/Str
   :backup-file-prefix-pattern s/Str
   :type-name s/Str})

(def BackupElement
  "The backup elements"
  (s/conditional
   #(= (:type %) :mysql)
   (merge
    BackupBaseElement
    {:db-user-name s/Str
     :db-user-passwd s/Str
     :db-name s/Str
     (s/optional-key :db-create-options) s/Str
     (s/optional-key :db-pre-processing) [s/Str]
     (s/optional-key :db-post-processing) [s/Str]})
   #(= (:type %) :file-compressed)
   (merge
    BackupBaseElement
    {:backup-path [directory-model/NonRootDirectory]
     (s/optional-key :new-owner) s/Str})
   #(= (:type %) :file-plain)
   (merge
     BackupBaseElement
     {:backup-path [directory-model/NonRootDirectory]
      (s/optional-key :new-owner) s/Str})
   #(= (:type %) :rsync)
   (merge
     BackupBaseElement
     {:backup-path [directory-model/NonRootDirectory]
      (s/optional-key :new-owner) s/Str})))


(def LocalManagement
  {:gens-stored-on-source-system s/Num})

(def TransportDuplicity
  {:tmp-dir s/Str
   :passphrase s/Str
   :public-key s/Str
   :private-key s/Str
   :gpg-key-id s/Str
   :days-stored-on-backup s/Num
   (s/optional-key :target-s3) {:aws-access-key-id s/Str
                                :aws-secret-access-key s/Str
                                :bucket-name s/Str
                                (s/optional-key :directory-name) s/Str}})

(def TransportManagement
  {(s/optional-key :ssh-pull) s/Any
   (s/optional-key :duplicity-push) TransportDuplicity})

(def ResolvedBackupConfig
  {:backup-name s/Str
   :backup-script-path s/Str
   :backup-transport-folder s/Str
   :backup-store-folder s/Str
   :backup-restore-folder s/Str
   :backup-user s/Keyword
   (s/optional-key :service-restart) s/Str
   :local-management LocalManagement
   :transport-management TransportManagement
   :backup-elements [BackupElement]})
```

## Compatibility
dda-pallet is compatible with the following versions
 * pallet 0.8
 * clojure 1.7
 * (x)ubunutu 16.0

## License
Copyright © 2015, 2016, 2017, 2018 meissa GmbH
Published under [apache2.0 license](LICENSE.md)
