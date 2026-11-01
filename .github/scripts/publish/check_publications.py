#!/usr/bin/env python3
"""Checks the publications in a Maven repository before they go to Maven Central, where a release is permanent.

    python3 .github/scripts/publish/check_publications.py ~/.m2/repository

Run after `./gradlew :admob:publishToMavenLocal` (the Publish workflow does). It fails when:
  - io.github.corneloaie:admob or one of its platform artifacts (admob-android, admob-iosarm64,
    admob-iossimulatorarm64) is missing, has a version other than gradle.properties', or more than one version;
  - an artifact lacks its POM, Gradle module file, sources jar or javadoc jar (Maven Central requires both jars);
  - a POM names a developer other than corneloaie, points its url or scm elsewhere, or isn't Apache 2.0;
  - a Gradle module variant is available at another group;
  - the root Gradle module has no swiftPMDependenciesMetadataElements variant, or the SwiftPM metadata it points to
    doesn't declare both Swift packages, GoogleMobileAds and UMP, as `from` the floors in gradle/libs.versions.toml.
    That variant is how an app inherits the iOS SDKs; an `exact` version means a -Padmob.pinIosSdkFloor build.
  - an iOS artifact lacks its cinterop klib, which carries the Obj-C API (GADNativeAd, ...) the public API exposes;
  - the Android POM doesn't depend on the GMA Next-Gen SDK and UMP at the versions in gradle/libs.versions.toml, or
    depends on the legacy play-services-ads, which clashes with the Next-Gen SDK.
"""
import json
import os
import re
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
GROUP = 'io.github.corneloaie'
ARTIFACT = 'admob'
PLATFORM_ARTIFACTS = ['admob-android', 'admob-iosarm64', 'admob-iossimulatorarm64']
IOS_ARTIFACTS = ['admob-iosarm64', 'admob-iossimulatorarm64']
DEVELOPER = 'corneloaie'
URL = 'https://github.com/corneloaie/admob-multiplatform'
LICENSE = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
SWIFTPM_VARIANT = 'swiftPMDependenciesMetadataElements'
# Package URL -> (product, gradle/libs.versions.toml version key)
SWIFT_PACKAGES = {
    'https://github.com/googleads/swift-package-manager-google-mobile-ads.git': ('GoogleMobileAds', 'google-mobile-ads-ios'),
    'https://github.com/googleads/swift-package-manager-google-user-messaging-platform.git':
        ('GoogleUserMessagingPlatform', 'google-ump-ios'),
}
# Maven module -> gradle/libs.versions.toml version key: the Android SDKs admob-android brings.
ANDROID_SDKS = {
    'com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk': 'ads-mobile-sdk',
    'com.google.android.ump:user-messaging-platform': 'user-messaging-platform',
}
LEGACY_ANDROID_SDKS = ['com.google.android.gms:play-services-ads', 'com.google.android.gms:play-services-ads-lite']
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}


def expected_version():
    match = re.search(r'^version=(.+)$', (ROOT / 'gradle.properties').read_text(), re.MULTILINE)
    return match.group(1).strip() if match else None


def catalog_version(key):
    match = re.search(rf'^{re.escape(key)}\s*=\s*"([^"]+)"', (ROOT / 'gradle/libs.versions.toml').read_text(), re.MULTILINE)
    return match.group(1) if match else None


def check_pom(path, problems):
    pom = ElementTree.parse(path).getroot()
    developers = [d.findtext('m:id', namespaces=NS) for d in pom.iterfind('.//m:developers/m:developer', NS)]
    if developers != [DEVELOPER]:
        problems.append(f'{path.name}: developers are {developers}, expected [{DEVELOPER!r}]')
    for field in ('m:url', 'm:scm/m:url'):
        if pom.findtext(field, namespaces=NS) != URL:
            problems.append(f'{path.name}: {field[2:]} is {pom.findtext(field, namespaces=NS)!r}, expected {URL!r}')
    licenses = [license.findtext('m:url', namespaces=NS) for license in pom.iterfind('.//m:licenses/m:license', NS)]
    if licenses != [LICENSE]:
        problems.append(f'{path.name}: licenses are {licenses}, expected [{LICENSE!r}]')
    for field in ('m:name', 'm:description'):
        if not pom.findtext(field, namespaces=NS):
            problems.append(f'{path.name}: no {field[2:]}')


def check_module(path, problems):
    module = json.loads(path.read_text())
    for variant in module.get('variants', []):
        available = variant.get('available-at')
        if available and available.get('group') != GROUP:
            problems.append(f'{path.name}: variant {variant["name"]} is available at {available.get("group")}:{available.get("module")}')
    return module


def check_swiftpm_metadata(module, directory, problems):
    variant = next((v for v in module.get('variants', []) if v['name'] == SWIFTPM_VARIANT), None)
    if variant is None:
        problems.append(f'{ARTIFACT}: no {SWIFTPM_VARIANT} variant, so apps would not inherit the iOS SDKs')
        return []
    files = [directory / f['url'] for f in variant.get('files', [])]
    if len(files) != 1 or not files[0].is_file():
        problems.append(f'{ARTIFACT}: {SWIFTPM_VARIANT} points to {[f.name for f in files]}, which are not all published')
        return []
    metadata = json.loads(files[0].read_text())
    found = {}
    for dependency in metadata.get('dependencies', []):
        url = dependency.get('repository', {}).get('value')
        version = dependency.get('version', {})
        products = [p.get('name') for p in dependency.get('products', [])]
        found[url] = (version.get('type', '').rsplit('.', 1)[-1], version.get('value'), products)
    for url, (product, key) in SWIFT_PACKAGES.items():
        expected = ('From', catalog_version(key), [product])
        if found.get(url) != expected:
            problems.append(f'{ARTIFACT}: SwiftPM metadata declares {url} as {found.get(url)}, expected {expected}')
    for url in found.keys() - SWIFT_PACKAGES.keys():
        problems.append(f'{ARTIFACT}: SwiftPM metadata declares an unexpected package {url}')
    return [f'{product} {found[url][0].lower()} {found[url][1]}' for url, (product, _) in SWIFT_PACKAGES.items() if url in found]


def check_android_sdks(path, problems):
    pom = ElementTree.parse(path).getroot()
    dependencies = {
        f"{d.findtext('m:groupId', namespaces=NS)}:{d.findtext('m:artifactId', namespaces=NS)}": d.findtext('m:version', namespaces=NS)
        for d in pom.iterfind('.//m:dependencies/m:dependency', NS)
    }
    sdks = []
    for module, key in ANDROID_SDKS.items():
        expected = catalog_version(key)
        if dependencies.get(module) != expected:
            problems.append(f'{path.name}: {module} is {dependencies.get(module)!r}, expected {expected!r}')
        sdks.append(f'{module.split(":")[1]} {dependencies.get(module)}')
    for module in LEGACY_ANDROID_SDKS:
        if module in dependencies:
            problems.append(f'{path.name}: depends on the legacy {module}')
    return sdks


def main():
    repository = Path(sys.argv[1]).expanduser() / Path(*GROUP.split('.'))
    version = expected_version()
    problems = []
    packages = []
    android_sdks = []
    files = 0
    for artifact in [ARTIFACT] + PLATFORM_ARTIFACTS:
        versions = sorted(v.name for v in (repository / artifact).iterdir() if v.is_dir()) if (repository / artifact).is_dir() else []
        if versions != [version]:
            problems.append(f'{artifact}: published versions are {versions}, expected [{version!r}]')
        directory = repository / artifact / version
        if not directory.is_dir():
            continue
        files += sum(1 for f in directory.iterdir() if f.is_file())
        prefix = f'{artifact}-{version}'
        for suffix in ('.pom', '.module', '-sources.jar', '-javadoc.jar'):
            if not (directory / f'{prefix}{suffix}').is_file():
                problems.append(f'{artifact}: no {prefix}{suffix}')
        if (directory / f'{prefix}.pom').is_file():
            check_pom(directory / f'{prefix}.pom', problems)
        if (directory / f'{prefix}.module').is_file():
            module = check_module(directory / f'{prefix}.module', problems)
            if artifact == ARTIFACT:
                packages = check_swiftpm_metadata(module, directory, problems)
        if artifact == 'admob-android' and (directory / f'{prefix}.pom').is_file():
            android_sdks = check_android_sdks(directory / f'{prefix}.pom', problems)
        if artifact in IOS_ARTIFACTS and not list(directory.glob(f'{prefix}-cinterop-*.klib')):
            problems.append(f'{artifact}: no cinterop klib')

    print(
        f'{GROUP}:{ARTIFACT}:{version}, {1 + len(PLATFORM_ARTIFACTS)} artifacts, {files} files; '
        f'Android SDKs: {", ".join(android_sdks) or "-"}; Swift packages: {", ".join(packages) or "-"}'
    )
    for problem in problems:
        print(f'::error::{problem}' if 'GITHUB_ACTIONS' in os.environ else f'error: {problem}')
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
