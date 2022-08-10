package com.example.customui.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import com.example.customui.BuildConfig
import com.example.customui.databinding.FragmentChatBinding
import com.nanorep.convesationui.structure.controller.ChatController
import com.nanorep.convesationui.structure.controller.ChatEventListener
import com.nanorep.nanoengine.bot.BotAccount
import com.nanorep.nanoengine.bot.BotChat
import com.nanorep.nanoengine.bot.BotChatListener
import com.nanorep.nanoengine.model.conversation.statement.*
import com.nanorep.sdkcore.model.StatementScope
import com.nanorep.sdkcore.utils.NRError


class ChatFragment : Fragment(), ChatEventListener {

    private var _binding: FragmentChatBinding? = null
    val binding get() = _binding!!
    val apiKey = BuildConfig.ApiKey
    val kb = BuildConfig.KB
    val server = BuildConfig.Server
    val name = BuildConfig.Account


    val account = BotAccount(apiKey,name,kb,server).apply { userId }
    private lateinit var botChat: BotChat
    var listener: BotChatListener? = null
    private var chatController: ChatController? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createChat()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chatBox.setEndIconOnClickListener {
            val userMessage = binding.editText.text
            sendStatementToEngine(userMessage.toString())
            userMessage?.clear()
            it.hideKeyboard()
            binding.linearLayout.removeAllViews()

        }
    }

    private fun createChat() {
        botChat = BotChat(account)
        listener = object : BotChatListener {
            override fun conversationIdUpdated(conversationId: String) {

            }

            override fun onResponse(response: StatementResponse) {
                handleStatementResponse(response)
                binding.botResponse.text = response.text
                Log.d("engine_response", response.text)

                if (response.optionsHandler.hasQuickOptions()) {
                    val quickOptions = response.optionsHandler.quickOptions

                    if (quickOptions != null) {
                        //binding.botMultiResponse.text = quickOptions.joinToString { it.getText() }
                        showButtons(quickOptions, response)

                    }
                    quickOptions?.joinToString { it.getText() }
                        ?.let { Log.d("actions_response", it) }
                }

            }

            override fun onError(error: NRError) {
                super.onError(error)
                Log.e("error", "createConversation: Failed to create conversation, $error")
            }
        }
        botChat.setBotChatListener(listener)
        botChat.initConversation()
    }

    private fun showButtons(quickOptions: List<QuickOption>, response: StatementResponse) {
        // add a button for every item in the quickOptions list
        for (i in quickOptions.indices) {
            val buttons = arrayOfNulls<Button>(quickOptions.size)
            val button = Button(requireContext())
            val layout = binding.linearLayout
            val buttonText = quickOptions[i].getText()
            buttons[i] = button
            buttons[i]?.text = buttonText
            layout.orientation = LinearLayout.HORIZONTAL
            layout.addView(buttons[i])
            //click listener for quickoption buttons
            button.setOnClickListener {
                response.statement = buttonText
                sendStatementToEngine(response.statement!!)
                binding.botResponse.text = response.text
                binding.linearLayout.removeAllViews()
            }

        }

    }


    private fun sendStatementToEngine(statement: String) {
        Log.d("botChat", "sendStatementToEngine(), statement = $statement")
        botChat.postStatement(
            StatementFactory.create(
                OutgoingStatement(
                    statement,
                    StatementScope.NanoBotScope,
                    InputMethod.ManuallyTyped.toInputSource()
                )
            )
        )
    }

    private fun handleStatementResponse(response: StatementResponse?) {
        botAnswer(response)

    }

    private fun botAnswer(response: StatementResponse?) {
        Log.d("bot_response", "botAnswer(), response = ${response.toString()}")
        var botAnswer: String? = response?.text

        botAnswer
    }

    fun View.hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

}
