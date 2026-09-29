Step 0.1 — SQLite dependency
=============================

This project needs the SQLite JDBC driver jar in this folder:

    lib/sqlite-jdbc-3.46.1.3.jar

Download it from Maven Central and place it here:
https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar

(Any recent 3.4x.x version works; just keep the filename/version
consistent with what you download, or update .classpath to match.)

Why this file exists:
The build/compile environment used to prepare this change had no
internet access, so the jar could not be downloaded and the project
could not actually be compiled or run here. The .classpath entry
below already points at this exact path, so once you drop the jar
in, Eclipse (or javac -cp) will pick it up with no further changes.

After adding the jar:
1. Refresh the project in Eclipse (or re-run javac with lib/*.jar on
   the module path).
2. Confirm module-info.java's `requires org.xerial.sqlitejdbc;`
   matches the Automatic-Module-Name in the jar's MANIFEST.MF. For
   the official org.xerial:sqlite-jdbc artifact this is
   "org.xerial.sqlitejdbc" — verify this once the jar is in hand,
   since it was not possible to inspect the actual manifest here.
