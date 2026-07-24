allprojects {
    tasks.withType<Test> {
        useJUnitPlatform()
        include("**/*Test.class", "**/*Tests.class", "**/*IT.class")
    }
}
