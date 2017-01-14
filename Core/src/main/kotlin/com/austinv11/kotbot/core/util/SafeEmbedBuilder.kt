package com.austinv11.kotbot.core.util

import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.util.EmbedBuilder
import java.awt.Color
import java.time.LocalDateTime
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.memberProperties

class SafeEmbedBuilder : EmbedBuilder() {
    
    val superEmbed: EmbedObject
        get() {
            val embedField = this::class.memberProperties.find { it.name == "embed" }!!
            embedField.isAccessible = true
            return embedField.get(this) as EmbedObject
        }

    override fun withFooterIcon(iconUrl: String?): EmbedBuilder {
        super.withFooterIcon(iconUrl)
        return this
    }

    override fun ignoreNullEmptyFields(): EmbedBuilder {
        super.ignoreNullEmptyFields()
        return this
    }

    override fun withColor(color: Color?): EmbedBuilder {
        super.withColor(color)
        return this
    }

    override fun withColor(color: Int): EmbedBuilder {
        super.withColor(color)
        return this
    }

    override fun withColor(r: Int, g: Int, b: Int): EmbedBuilder {
        super.withColor(r, g, b)
        return this
    }

    override fun withImage(imageUrl: String?): EmbedBuilder {
        super.withImage(imageUrl)
        return this
    }

    override fun withThumbnail(url: String?): EmbedBuilder {
        super.withThumbnail(url)
        return this
    }

    override fun withAuthorUrl(url: String?): EmbedBuilder {
        super.withAuthorUrl(url)
        return this
    }

    override fun withAuthorName(name: String?): EmbedBuilder {
        super.withAuthorName(name)
        return this
    }

    override fun withAuthorIcon(url: String?): EmbedBuilder {
        super.withAuthorIcon(url)
        return this
    }

    override fun withUrl(url: String?): EmbedBuilder {
        super.withUrl(url)
        return this
    }

    override fun appendDescription(desc: String?): EmbedBuilder {
        return withDescription(superEmbed.description+desc)
    }

    override fun appendDesc(desc: String?): EmbedBuilder {
        return withDescription(superEmbed.description+desc)
    }

    override fun withDescription(desc: String?): EmbedBuilder {
        super.withDescription(desc?.coerce(DESCRIPTION_CONTENT_LIMIT))
        return this
    }

    override fun withDesc(desc: String?): EmbedBuilder {
        return withDescription(desc)
    }

    override fun withFooterText(footer: String?): EmbedBuilder {
        super.withFooterText(footer?.coerce(FOOTER_CONTENT_LIMIT))
        return this
    }

    override fun withTitle(title: String?): EmbedBuilder {
        super.withTitle(title?.coerce(TITLE_LENGTH_LIMIT))
        return this
    }

    override fun appendField(title: String?, content: String?, inline: Boolean): EmbedBuilder {
        if (superEmbed.fields.size != FIELD_COUNT_LIMIT)
            super.appendField(title?.coerce(TITLE_LENGTH_LIMIT), content?.coerce(FIELD_CONTENT_LIMIT), inline)
        return this
    }

    override fun withTimestamp(ldt: LocalDateTime?): EmbedBuilder {
        super.withTimestamp(ldt)
        return this
    }

    override fun withTimestamp(millis: Long): EmbedBuilder {
        super.withTimestamp(millis)
        return this
    }
}
