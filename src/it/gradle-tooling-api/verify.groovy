import java.util.jar.JarFile

def gradleToolingApiJar = new File(projectBuildDirectory,
    "local-repo/org/gradle/gradle-tooling-api/${versionGradle8}/gradle-tooling-api-${versionGradle8}.jar")
assert gradleToolingApiJar.exists()
def jar = new JarFile(gradleToolingApiJar)
def containsManifest = false
def containsGradleApi = false
jar.entries().each {
    if (it.name == 'META-INF/MANIFEST.MF') {
        containsManifest = true
    }
    if (it.name.startsWith('org/gradle/api/')) {
        containsGradleApi = true
    }
}
assert containsManifest
assert containsGradleApi
