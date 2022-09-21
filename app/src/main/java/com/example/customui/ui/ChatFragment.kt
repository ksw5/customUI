package com.example.customui.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.customui.BuildConfig
import com.example.customui.databinding.FragmentChatBinding
import com.nanorep.convesationui.structure.controller.ChatEventListener
import com.nanorep.nanoengine.Entity
import com.nanorep.nanoengine.NRAction
import com.nanorep.nanoengine.PersonalInfoRequest
import com.nanorep.nanoengine.bot.BotAccount
import com.nanorep.nanoengine.bot.BotChat
import com.nanorep.nanoengine.bot.BotChatListener
import com.nanorep.nanoengine.model.conversation.SessionInfoConfigKeys
import com.nanorep.nanoengine.model.conversation.isTextOnly
import com.nanorep.nanoengine.model.conversation.statement.InputMethod
import com.nanorep.nanoengine.model.conversation.statement.OutgoingStatement
import com.nanorep.nanoengine.model.conversation.statement.StatementFactory
import com.nanorep.nanoengine.model.conversation.statement.StatementResponse
import com.nanorep.nanoengine.nonbot.EntitiesProvider
import com.nanorep.sdkcore.model.StatementScope
import com.nanorep.sdkcore.utils.Completion
import com.nanorep.sdkcore.utils.NRError


class ChatFragment : Fragment(), ChatEventListener {


    private var _binding: FragmentChatBinding? = null
    val binding get() = _binding!!
    val apiKey = BuildConfig.ApiKey
    val kb = BuildConfig.KB
    val server = BuildConfig.Server
    val name = BuildConfig.Account


    val account = BotAccount(apiKey, name, kb, server)
    var textOnly = account.info.isTextOnly

    private lateinit var botChat: BotChat
    private lateinit var listener: BotChatListener



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createChat()
        textOnly = true

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentChatBinding.inflate(inflater, container, false)

        val list = arrayOf("amazon", "google", "apple", "meta", "netflix")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
        binding.autoComplete.setAdapter(adapter)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chatBox.setEndIconOnClickListener {
            val userMessage = binding.autoComplete.text
            sendStatementToEngine(userMessage.toString())
            userMessage?.clear()
            it.hideKeyboard()
            binding.linearLayout.removeAllViews()

        }


    }


    private fun createChat() {
        botChat = BotChat(account)


        listener = object : BotChatListener {

            override fun conversationIdUpdated(conversationId: String) {}

            override fun onResponse(response: StatementResponse) {

                handleStatementResponse(response)
                Log.d("response_type", "${response.responseType}")
                //val formattedText = Html.fromHtml(response.text).toString()
                binding.botResponse.text = response.text


                val responseOptionsHandler = response.optionsHandler

                if (response.responseType == "carousel") {
                    Log.d("carousel", "carousel here")
                    showCarousel(response)
                }

                if (responseOptionsHandler.hasQuickOptions()) {
                    showButtons(response)
                }
            }



            override fun onError(error: NRError) {
                super.onError(error)
                Log.e("error", "createConversation: Failed to create conversation, $error")
            }
        }

        botChat.setBotChatListener(listener)
        botChat.setEntitiesProvider(object : EntitiesProvider {
            override fun provide(personalInfoRequest: PersonalInfoRequest, callback: PersonalInfoRequest.Callback) {
                when (personalInfoRequest.id) { // this is the formatted hidden method that was injected into the article inside a "{{}}"
                    "NewLoan" -> {
                callback.onInfoReady("", NRAction(personalInfoRequest.id).apply {
                    Log.d("personalInfo", "${personalInfoRequest.id} ${personalInfoRequest.userInfo}")
                    if (userInfo != null) {
                        personalInfoRequest.userInfo?.let{ info -> this.userInfo.putAll(info)}
                        //this.userInfo.putAll(personalInfoRequest.userInfo)
                    }

                }) // the action is kind of parallel retrieved info - The customer should set it according to its needs
                     // this action will be added to the actions list on the StatementResponse
                  }

                }
            }

            override fun provide(info: ArrayList<String>, callback: Completion<ArrayList<Entity>>) {

            }

        })
        account.info.addConfigurations(SessionInfoConfigKeys.IsTextOnly to true)

        botChat.initConversation()



    }
    //אני רוצה לקחת הלוואה

    //אני רוצה הלוואה

    //אני רוצה לקחת הלוואה


    private fun showButtons(response: StatementResponse) {

        //val responseTest = response.actions + response.optionsHandler.quickOptions

        val quickOptions = response.optionsHandler.quickOptions
        // add a button for every item in the quickOptions list
        val buttons = arrayListOf<Button>()
        quickOptions?.forEach { quickOption ->
            val channel = quickOption.nRchannel
            val quickOptionText = quickOption.getText()
            val button = Button(requireContext()).apply {
                text = quickOptionText
                this.setOnClickListener {
                    response.statement = quickOptionText
                    sendStatementToEngine(response.statement!!)
                    binding.botResponse.text = response.text
                    binding.linearLayout.removeAllViews()
                }
            }
            val layout = binding.linearLayout

            buttons.add(button)

            layout.orientation = LinearLayout.HORIZONTAL
            layout.addView(button)
        }
    }

    private fun showCarousel(response: StatementResponse) {
        val gallery = response.multiAnswers
        val layout = binding.linearLayout
        val topLinearLayout = LinearLayout(requireContext())
        val scrollView = HorizontalScrollView(requireContext())
        gallery?.forEach { image ->
            val galleryText = image.title
            topLinearLayout.orientation = LinearLayout.HORIZONTAL
            val imageView = ImageView(requireContext())
            //topLinearLayout.orientation = HORIZONTAL

            val imagesUrls = arrayListOf<String?>()
            imagesUrls.add(image.imageUrl)
            Log.d("image urls", imagesUrls.toString())
            imagesUrls.forEach {

                imageView.apply {
                    tag = galleryText
                    //val imageLayoutParams = LinearLayout.LayoutParams(2000, 2000)
                    //setLayoutParams(imageLayoutParams)
                }
                GlideApp
                    .with(requireContext())
                    .load(it)
                    .override(1000, 2000)
                    .into(imageView)

            }
            topLinearLayout.addView(imageView)
        }
        scrollView.apply {
            isFillViewport = true
            addView(topLinearLayout)
        }
        layout.addView(scrollView)

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

        response?.statement

        //var botAnswer: String? = response?.text
    }

    fun View.hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
