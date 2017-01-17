package com.austinv11.kotbot.core.api.commands

import com.austinv11.kotbot.core.CLIENT
import com.austinv11.kotbot.core.config.Config
import com.austinv11.kotbot.core.util.CommandContext
import com.austinv11.kotbot.core.util.buffer
import com.austinv11.kotbot.core.util.contextMap
import com.austinv11.kotbot.core.util.isNullableSubtypeOf
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.IListener
import sx.blah.discord.api.internal.DiscordEndpoints
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.*
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.MessageBuilder
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

object CommandRegistry {
    
    val commands = mutableListOf<Command>()
    
    fun init(client: IDiscordClient) {
        client.dispatcher.registerListener(CommandListener())
    }
    
    class CommandListener: IListener<MessageReceivedEvent> {
        
        override fun handle(event: MessageReceivedEvent) { //I hate command parsing
            try {
                //Figuring out if a message is a command call
                if (!event.message.content.isNullOrEmpty()) {
                    val tokenizer = event.message.tokenize()
                    if (tokenizer.hasNextChar()) {
                        val char = tokenizer.nextChar()
                        if (char != Config.command_prefix) { //Message did not start with the command prefix, maybe it's a direct mention?
                            tokenizer.stepTo(0)
                            if (!tokenizer.hasNextMention())
                                return //Nope

                            val mention = tokenizer.nextMention()
                            if (!(mention.startIndex == 0
                                    && (mention.mentionObject is IUser)
                                    && mention.mentionObject == CLIENT.ourUser)) {
                                return //Nope
                            }
                        }

                        val commandName = tokenizer.nextWord().content

                        //Getting the command
                        val command = commands.firstOrNull { it.doesCommandMatch(commandName) } ?: return

                        if (command.requiredLevel.ordinal > event.author.retrievePermissionLevel(event.guild).ordinal) {
                            buffer { event.channel.sendMessage(Config.missing_permission_message.format(command.requiredLevel.toString())) }
                            return
                        }

                        val remainingContent = tokenizer.remainingContent
                        val nargs = if (tokenizer.hasNext()) remainingContent.split(" ").size else 0

                        val executors = command::class.declaredFunctions
                                .filter {
                                    it.findAnnotation<Executor>() != null
                                            && (it.valueParameters.size <= nargs || it.minimumParamCount <= nargs)
                                }

                        val hash = Objects.hash(Thread.currentThread(), command.javaClass.name)
                        contextMap[hash] = CommandContext(event.message)

                        try {
                            if (nargs == 0 && executors.isEmpty()) { //Time for fluent execution :D
                                TODO("Fluent interpretation is not available yet")
                            } else if (executors.isNotEmpty()) {
                                executors.sortedWith(Comparator<KFunction<*>> { o1, o2 -> -(o1.valueParameters.size.compareTo(o2.valueParameters.size)) })
                                        .forEach {
                                            val split = splitToNArgs(remainingContent, it.valueParameters.size)
                                            val invocationResult = invokeFunction(event.message, command, it, split)
                                            if (invocationResult != null) {
                                                buffer { invocationResult.parseToMessage(event.channel) }
                                                return@handle
                                            }
                                        }
                                throw IllegalArgumentException("Unable to properly handle provided arguments!")
                            } else {
                                buffer { event.channel.sendMessage(Config.command_error_format.format("Cannot execute command with $nargs args!")) }
                            }
                        } finally {
                            contextMap.remove(hash)
                        }
                    }
                }
            } catch (e: Throwable) {
                buffer { event.channel.sendMessage(Config.command_error_format.format(e.message)) }
                
                if (e !is CommandException)
                    e.printStackTrace()
            }
        }
        
        private val KFunction<*>.minimumParamCount: Int
            get() = this.valueParameters.filter { !it.isOptional }.size
        
        private fun splitToNArgs(value: String, nargs: Int): Array<String?> {
            if (nargs < 1)
                return arrayOf()
            else if (nargs == 1)
                return arrayOf(value)
            else {
                val list = mutableListOf<String?>()
                value.split(" ").forEachIndexed { i, str -> 
                    if (i < nargs-1) {
                        list.add(str)
                    } else if (i == nargs-1) {
                        list.add(str)
                    } else {
                        list.add(list[nargs-1] + " " + str)
                    }
                }
                while (list.size < nargs) {
                    list.add(null)
                }
                return list.toTypedArray()
            }
        }
        
        private fun invokeFunction(context: IMessage, instance: Any, function: KFunction<*>, args: Array<String?>): ObjectHolder<Any?>? {
            try {
                val convertedParams = mutableMapOf<KParameter, Any?>()
                function.isAccessible = true
                function.valueParameters.forEachIndexed { i, param -> 
                    if (!(param.isOptional || param.type.isMarkedNullable) && args[i] == null)
                        throw Exception()
                    else if ((param.isOptional || param.type.isMarkedNullable) && args[i] == null) {
                        if (param.type.isMarkedNullable)
                            convertedParams.put(param, null)
                    } else {
                        convertedParams.put(param, convert(context, param.type, args[i]))
                    }
                }
                convertedParams.put(function.instanceParameter!!, instance)
                return ObjectHolder(function.callBy(convertedParams))
            } catch (e: Throwable) {
                if (e is InvocationTargetException) {
                    throw e.targetException
                } else if (e is CommandException)
                    throw e
                return null
            }
        }
        
        private fun convert(context: IMessage, desiredType: KType, value: String?): Any? {
            if (value == null || value.isEmpty())
                return null
            
            if (desiredType.isNullableSubtypeOf(String::class.starProjectedType)) {
                return value
            } else if (desiredType.isNullableSubtypeOf(Number::class.starProjectedType)) {
                if (desiredType.isNullableSubtypeOf(Int::class.starProjectedType)) {
                    return value.toInt()
                } else if (desiredType.isNullableSubtypeOf(Double::class.starProjectedType)) {
                    return value.toDouble() 
                } else if (desiredType.isNullableSubtypeOf(Long::class.starProjectedType)) {
                    return value.toLong()
                } else if (desiredType.isNullableSubtypeOf(Float::class.starProjectedType)) {
                    return value.toFloat()
                } else if (desiredType.isNullableSubtypeOf(Byte::class.starProjectedType)) {
                    return value.toByte()
                } else if (desiredType.isNullableSubtypeOf(Short::class.starProjectedType)) {
                    return value.toShort()
                } else {
                    return null
                }
            } else if (desiredType.isNullableSubtypeOf(Boolean::class.starProjectedType)) {
                return value.toBoolean()
            } else if (desiredType.isNullableSubtypeOf(Char::class.starProjectedType)) {
                return value.toCharArray()[0]
            } else if (desiredType.isNullableSubtypeOf(IVoiceChannel::class.starProjectedType)) {
                return context.guild.getVoiceChannelByID(value) 
                        ?: context.guild.getVoiceChannelsByName(value)?.firstOrNull() 
                        ?: CLIENT.getVoiceChannelByID(value) 
                        ?: CLIENT.voiceChannels.filter { it.name.equals(value, true) }.firstOrNull()
            } else if (desiredType.isNullableSubtypeOf(IChannel::class.starProjectedType)) {
                val value = value.removePrefix("<#").removeSuffix(">")
                return context.guild.getChannelByID(value) 
                        ?: CLIENT.getChannelByID(value) 
                        ?: context.guild.getChannelsByName(value)?.firstOrNull() 
                        ?: CLIENT.channels.filter { it.name.equals(value, true) }.firstOrNull()
            } else if (desiredType.isNullableSubtypeOf(IGuild::class.starProjectedType)) {
                return CLIENT.getGuildByID(value) 
                        ?: CLIENT.guilds.filter { it.name.equals(value, true) }.firstOrNull()
            } else if (desiredType.isNullableSubtypeOf(IUser::class.starProjectedType)) {
                val value = value.removePrefix("<@").removeSuffix(">").removePrefix("!")
                return context.guild.getUserByID(value) 
                        ?: CLIENT.getUserByID(value) 
                        ?: context.guild.getUsersByName(value, true)?.firstOrNull()
                        ?: CLIENT.users.filter { it.name.equals(value, true) }.firstOrNull()
            } else if (desiredType.isNullableSubtypeOf(IMessage::class.starProjectedType)) {
                return context.guild.getMessageByID(value) ?: CLIENT.getMessageByID(value)
            } else if (desiredType.isNullableSubtypeOf(IRole::class.starProjectedType)) {
                val value = value.removePrefix("<@&").removeSuffix(">")
                if (value == "@everyone")
                    return context.guild.everyoneRole
                return context.guild.getUserByID(value) 
                        ?: CLIENT.getUserByID(value)
                        ?: context.guild.getRolesByName(value)?.firstOrNull()
                        ?: CLIENT.roles.filter { it.name.equals(value, true) }.firstOrNull()
            } else if (desiredType.isNullableSubtypeOf(IEmoji::class.starProjectedType)) {
                val value = value.removePrefix("<:").removeSuffix(">").split(":")[0]
                return context.guild.getEmojiByID(value) 
                        ?: context.guild.getEmojiByID(value) 
                        ?: context.guild.getEmojiByName(value)
            } else if (desiredType.isNullableSubtypeOf(IInvite::class.starProjectedType)) {
                return CLIENT.getInviteForCode(value)
            } else if (desiredType.jvmErasure.java.isEnum) {
                try {
                    return desiredType.jvmErasure.java.getMethod("valueOf", String::class.java).invoke(null, value.toUpperCase())
                } catch (e: Throwable) {
                    return null
                }
            } else {
                return null
            }
        }
        
        data class ObjectHolder<out T>(val any: T) {
            
            fun parseToMessage(channel: IChannel): IMessage? {
                if (any == null)
                    return null
                
                if (any is String) {
                    return channel.sendMessage(any)
                } else if (any is Boolean) {
                    return channel.sendMessage(if (any) Config.default_command_success_message else Config.default_command_failure_message)
                } else if (any is Number) {
                    return channel.sendMessage(any.toString())
                } else if (any is Char) {
                    return channel.sendMessage(any.toString())
                } else if (any is Array<*>) {
                    return channel.sendMessage(Arrays.toString(any))
                } else if (any is Iterable<*>) {
                    return channel.sendMessage(any.toString())
                } else if (any is IMessage) {
                    return null
                } else if (any is IChannel) {
                    return channel.sendMessage(any.mention())
                } else if (any is IRole) {
                    return channel.sendMessage(any.mention())
                } else if (any is IGuild) {
                    return channel.sendMessage(any.name)
                } else if (any is IUser) {
                    return channel.sendMessage(any.mention())
                } else if (any is IInvite) {
                    return channel.sendMessage(DiscordEndpoints.INVITE+any.inviteCode)
                } else if (any is MessageBuilder) {
                    return any.build()
                } else if (any is IEmoji) {
                    return channel.sendMessage(any.toString())
                } else if (any is EmbedBuilder) {
                    return channel.sendMessage(any.build())
                } else if (any is EmbedObject) {
                    return channel.sendMessage(any)
                } else {
                    return channel.sendMessage(any.toString())
                }
            }
        }
    }
}
