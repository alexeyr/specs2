_This project has been made public to facilitate its review before official release, but it's not released yet! _

Installation instructions
=========================

You need to download and install sbt (simple build tool)
Then execute the following command:

        > sbt update publish-local

Then you can generate the User Guide with:

        sbt
        > test-ony org.specs2.UserGuide -- html

This should create html files in the target/specs2-reports directory. 
