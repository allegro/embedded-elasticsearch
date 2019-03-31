package pl.allegro.tech.embeddedelasticsearch

import spock.lang.Specification

class InstallationDescriptionSpec extends Specification {

    def "should detect previous version with only digits when first digit is smaller"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("1.7.5", "5.5.0")
        then:
            !result
    }

    def "should detect previous alphanumeric version when first digit is smaller"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("1.7.5-rc1", "5.5.0")
        then:
            !result
    }

    def "should detect previous version with only digits when second digit is smaller"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("2.4.5", "5.5.0")
        then:
            !result
    }

    def "should detect previous alphanumeric version when second digit is smaller"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("2.4.5-rc1", "5.5.0")
        then:
            !result
    }
    
    def "should detect previous version with only digits when third digit is smaller"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("5.5.0", "5.5.5")
        then:
            !result
    }

    def "should detect previous alphanumeric version when third digit is smaller"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("5.5.0-rc1", "5.5.5")
        then:
            !result
    }
    
    def "should detect next version with only digits when first digit is bigger"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("6.7.5", "5.5.0")
        then:
            result
    }

    def "should detect next alphanumeric version when first digit is bigger"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("6.7.5-rc1", "5.5.0")
        then:
            result
    }

    def "should detect next version with only digits when second digit is bigger"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("5.6.5", "5.5.0")
        then:
            result
    }

    def "should detect next alphanumeric version when second digit is bigger"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("5.6.5-rc1", "5.5.0")
        then:
            result
    }
    
    def "should detect next version with only digits when third digit is bigger"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("5.5.6", "5.5.5")
        then:
            result
    }

    def "should detect next alphanumeric version when third digit is bigger"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("5.5.6-rc1", "5.5.5")
        then:
            result
    }
    
    def "should detect same version with only digits"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("5.5.5", "5.5.5")
        then:
            result
    }

    def "should detect previous alphanumeric version"() {
        when:
            final result = InstallationDescription.versionMatchOrAfter("5.5.5-rc1", "5.5.5")
        then:
            !result
    }
}
