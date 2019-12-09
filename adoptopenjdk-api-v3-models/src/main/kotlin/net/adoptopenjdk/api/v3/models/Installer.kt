package net.adoptopenjdk.api.v3.models

class Installer(name: String, link: String, size: Long, checksum: String?, checksum_link: String?, signature_link: String? = null) : Asset(name, link, size, checksum, checksum_link, signature_link)
