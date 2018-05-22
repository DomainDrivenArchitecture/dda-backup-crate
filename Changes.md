# Changes
## Changes made by 1.0.1
- made tar & restore more symmetric
- removed unused code
- removed hard coded root-pw for duplicity
- use selmer templates
- Added rsync
- fix :backup-file-prefix-pattern
- Remove root-dir and subdir-to-save configuration from domain

Since we use a new configuratin to store and restore our files, old backups will be placed in the root directory. This was the old configuration:
```clojure
{:root-dir "example-dir"      ;this is where we restored our BackupPath
 :subdir-to-save "subdir"     ;this is what we wanted to backup
 }  
```
And this was our old restore-script-tar part:
```clojure
(str "tar " tar-own-options "-x" tar-compress-option "f ${" (restore-dump-name element) "} -C " root-dir)
```
Now we only need to specify what we want to backup and our restore-scipt automatically places the backuped file in the right directory.
```clojure
{:backup-path ["saveme"]}     ;this is what we want to backup
 }  
```
And this is our new restore-script-tar part:
```bash
tar{% if tar-own-options %} {{tar-own-options}}{% endif %}{{tar-compress-option}} ${{restore-dump-name}} -C /

```

As u can see, the old way had the keyword `root-dir`. And we restored that file in the root-dir path. 
Now we just save the `backup-path` and restore it under `/backup-path`. 
Therefor all new backups will be automatically put in the right path. 
The only problem is, that when u have old backups and want to restore it, they will be put in `\` and u have to move them manually.
