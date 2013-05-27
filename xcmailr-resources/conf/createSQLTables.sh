java -cp ../lib/h2*.jar org.h2.tools.RunScript -user sa -url jdbc:h2:~/testDB -script default-drop.sql
java -cp ../lib/h2*.jar org.h2.tools.RunScript -user sa -url jdbc:h2:~/testDB -script default-create.sql
