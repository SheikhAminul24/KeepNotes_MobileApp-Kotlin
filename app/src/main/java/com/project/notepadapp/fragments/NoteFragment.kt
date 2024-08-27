package com.project.notepadapp.fragments

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.notepadapp.Models.GridModel
import com.project.notepadapp.Models.NoteModel
import com.project.notepadapp.R
import com.project.notepadapp.Utils.SwipeGesture
import com.project.notepadapp.Utils.hideKeyboard
import com.project.notepadapp.adapters.NoteAdapter
import com.project.notepadapp.ativities.MainActivity
import kotlinx.coroutines.*
import java.time.chrono.ChronoLocalDateTime


class NoteFragment : Fragment() {

    lateinit var addNoteFab:LinearLayout
    lateinit var noteUser:ImageView
    lateinit var search:EditText
    lateinit var chatFabText: TextView
    private var backPressTime: Long=0
    lateinit var rvNote: RecyclerView
    lateinit var noData:ImageView
    lateinit var options: FirestoreRecyclerOptions<NoteModel>
    lateinit var firebaseAdapter: NoteAdapter
    lateinit var noteGrid:ImageView
    private var isGrid=false

    var layoutManager=StaggeredGridLayoutManager(1,StaggeredGridLayoutManager.VERTICAL)
    private var backToast : Toast? =null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition=MaterialElevationScale(false).apply {
            duration=350
        }
        enterTransition=MaterialElevationScale(true).apply {
            duration = 350
        }


    }
//back press
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val callback=object :OnBackPressedCallback (true){
            override fun handleOnBackPressed(){
               if(backPressTime+2000>System.currentTimeMillis()){
                   backToast?.cancel()
                   activity?.moveTaskToBack( true)
                   activity?.finish()
                   return

               }
                else{
                    val backToast=Toast.makeText(context,"Double press to Exit",Toast.LENGTH_SHORT )
                   backToast.show()
               }
                backPressTime=System.currentTimeMillis()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,callback)

        val view= inflater.inflate(R.layout.fragment_note, container, false)
        rvNote=view.findViewById(R.id.rv_note)
        setUpFirebaseAdapter()

        return view


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController= Navigation.findNavController(view)
        requireView().hideKeyboard()
        CoroutineScope(Dispatchers.Main).launch {
            delay(10)
            activity?.window?.statusBarColor=resources.getColor(android.R.color.transparent)
            activity?.window?.navigationBarColor=resources.getColor(android.R.color.transparent)
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)


        }

        addNoteFab=view.findViewById(R.id.add_note_fab)
        rvNote=view.findViewById(R.id.rv_note)
        noData=view.findViewById(R.id.no_data)
        chatFabText=view.findViewById(R.id.chatFabText)
        noteUser=view.findViewById(R.id.note_user)
        search=view.findViewById(R.id.search)
        noteGrid=view.findViewById(R.id.note_grid)

        FirebaseFirestore.getInstance().collection("notes")
            .document(FirebaseAuth.getInstance().uid.toString()).addSnapshotListener{value,error->

            val data=value?.toObject(GridModel::class.java)
            if(data?.isGrid!=null){
                isGrid=data.isGrid

            }
            if(isGrid==true){
                layoutManager= StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL)
                rvNote.layoutManager=layoutManager

            }

            else{
                layoutManager= StaggeredGridLayoutManager(1,StaggeredGridLayoutManager.VERTICAL)
                rvNote.layoutManager=layoutManager
            }

//grid button er kaj
            noteGrid.setOnClickListener {


                if (isGrid == false) {
                    layoutManager =
                        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                    rvNote.layoutManager = layoutManager
                    val gridModel = GridModel()
                    isGrid = true
//firebase e grid
                    gridModel.isGrid = isGrid
                    FirebaseFirestore.getInstance().collection("notes")
                        .document(FirebaseAuth.getInstance().uid.toString()).set(gridModel)


                } else {
                    layoutManager =
                        StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
                    rvNote.layoutManager = layoutManager
                    val gridModel = GridModel()
                    isGrid = false

                    gridModel.isGrid = isGrid
                    FirebaseFirestore.getInstance().collection("notes")
                        .document(FirebaseAuth.getInstance().uid.toString()).set(gridModel)


                }
            }


        }
//user button e click korle ki hbe ..
        noteUser.setOnClickListener{
         val dialog= Dialog(requireContext(),R.style.BottomSheetDialogTheme)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.account_dialogue)
            dialog.show()
            val userProfile:ImageView=dialog.findViewById(R.id.user_profile)
            val userName:TextView=dialog.findViewById(R.id.user_name)
            val userMail:TextView=dialog.findViewById(R.id.user_mail)
            val userLogout:TextView=dialog.findViewById(R.id.user_logout)
//gmail dp
            Glide.with(requireContext()).load(FirebaseAuth.getInstance().currentUser?.photoUrl).into(userProfile)
//user name n gmail
            userName.text=FirebaseAuth.getInstance().currentUser?.displayName
            userMail.text=FirebaseAuth.getInstance().currentUser?.email
//logout
            userLogout.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                //mainativity te jabe
                startActivity(Intent(requireActivity(),MainActivity::class.java))
                requireActivity().finish()
            }


        }

//search
        search.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            noData.visibility=View.GONE

                setUpFirebaseAdapter()


            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {


                if(p0?.isEmpty()!!){
                    //adapter e pass
                    setUpFirebaseAdapter()
                }
                val query=FirebaseFirestore.getInstance().collection("notes").document(FirebaseAuth.getInstance().uid.toString())
                    .collection("myNotes").orderBy("title").startAt(p0.toString()).endAt(p0.toString()+"\uF8FF")
                options=FirestoreRecyclerOptions.Builder<NoteModel>().setQuery(query,NoteModel::class.java).setLifecycleOwner(viewLifecycleOwner).build()

                firebaseAdapter= NoteAdapter(options,requireActivity())
                recyclerViewSetUp()
            }

            override fun afterTextChanged(p0: Editable?) {
                if(p0?.isEmpty()!!){
                    setUpFirebaseAdapter()
                }
                else{

                }
            }

        })
//keyboard e likha for search
        search.setOnEditorActionListener{v,actionId,_->
            if(actionId==EditorInfo.IME_ACTION_SEARCH){
                v.clearFocus()
                requireView().hideKeyboard()

            }
            return@setOnEditorActionListener true

        }
//scroll to animate add note
        rvNote.setOnScrollChangeListener{ _,scrollX,scrollY, _,oldScrollY ->
            when{
               scrollX>oldScrollY->{
                   chatFabText.visibility=View.GONE
               }
                scrollX==scrollY->{
                    chatFabText.visibility=View.VISIBLE
                }
                else->{
                    chatFabText.visibility=View.VISIBLE

                }
            }

        }

        addNoteFab.setOnClickListener{
   //go to save edit frag
            navController.navigate(R.id.action_noteFragment_to_saveEditFragment)
        }

        swipeToGesture(rvNote)
        FirebaseFirestore.getInstance().collection("notes").document(FirebaseAuth.getInstance().uid.toString())
            .collection("myNotes").addSnapshotListener{value,error->
                if(value!!.isEmpty){
                    noData.visibility=View.VISIBLE
                }
                else{
                    noData.visibility=View.GONE
                }

            }

    }
//swipe del note
    private fun swipeToGesture(rvNote: RecyclerView?) {

        val swipeGesture=object :SwipeGesture(requireContext()){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                var actionBtnTapped=false
                val position=viewHolder.position
                val previousId=firebaseAdapter.snapshots.getSnapshot(position).id
                val note=firebaseAdapter.getItem(position)
//del note
                try {
                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            FirebaseFirestore.getInstance().collection("notes")
                                .document(FirebaseAuth.getInstance().uid.toString())
                                .collection("myNotes")
                                .document(firebaseAdapter.snapshots.getSnapshot(position).id)
                                .delete()

                            search.apply {

                                hideKeyboard()
                                clearFocus()
                            }

                            val snackbar = Snackbar.make(
                                requireView(), "Note Deleted", Snackbar.LENGTH_LONG
                            ).addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                override fun onDismissed(
                                    transientBottomBar: Snackbar?,
                                    event: Int
                                ) {
                                    super.onDismissed(transientBottomBar, event)
                                }
//undo deleted one
                                override fun onShown(transientBottomBar: Snackbar?) {

                                    transientBottomBar?.setAction("UNDO") {
                                        FirebaseFirestore.getInstance().collection("notes")
                                            .document(FirebaseAuth.getInstance().uid.toString())
                                            .collection("myNotes").document().set(note)
                                        actionBtnTapped = true

                                    }
                                    super.onShown(transientBottomBar)
                                }
//del er shomoy animation
                            }).apply {
                                animationMode = Snackbar.ANIMATION_MODE_FADE
                                setAnchorView(R.id.add_note_fab)
                            }
                            snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.orangeRed))
                            snackbar.show()
                        }
                        ItemTouchHelper.RIGHT ->{
                            FirebaseFirestore.getInstance().collection("notes")
                                .document(FirebaseAuth.getInstance().uid.toString())
                                .collection("myNotes")
                                .document(firebaseAdapter.snapshots.getSnapshot(position).id)
                                .delete()

                            search.apply {

                                hideKeyboard()
                                clearFocus()
                            }

                            val snackbar = Snackbar.make(
                                requireView(), "Note Deleted", Snackbar.LENGTH_LONG
                            ).addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                override fun onDismissed(
                                    transientBottomBar: Snackbar?,
                                    event: Int
                                ) {
                                    super.onDismissed(transientBottomBar, event)
                                }

                                override fun onShown(transientBottomBar: Snackbar?) {

                                    transientBottomBar?.setAction("UNDO") {
                                        FirebaseFirestore.getInstance().collection("notes")
                                            .document(FirebaseAuth.getInstance().uid.toString())
                                            .collection("myNotes").document().set(note)
                                        actionBtnTapped = true

                                    }
                                    super.onShown(transientBottomBar)
                                }

                            }).apply {
                                animationMode = Snackbar.ANIMATION_MODE_FADE
                                setAnchorView(R.id.add_note_fab)
                            }
                            snackbar.setActionTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.orangeRed
                                )
                            )
                            snackbar.show()
                        }
                        }


                     }
                    catch (e:Exception){
                        Toast.makeText(requireContext(),e.message,Toast.LENGTH_SHORT).show()
                    }

            }


        }

        val touchHelper=ItemTouchHelper(swipeGesture)

        touchHelper.attachToRecyclerView(rvNote)

    }

    private fun recyclerViewSetUp() {
        rvNote.setHasFixedSize(true)
        rvNote.adapter=firebaseAdapter
        rvNote.layoutManager
        rvNote.viewTreeObserver.addOnPreDrawListener {
            startPostponedEnterTransition()
            true
        }
    }

    private fun setUpFirebaseAdapter() {

        val query:Query=FirebaseFirestore.getInstance().collection("notes").document(FirebaseAuth.getInstance().uid.toString())
            .collection("myNotes").orderBy("date",Query.Direction.DESCENDING)
        options=FirestoreRecyclerOptions.Builder<NoteModel>().setQuery(query,NoteModel::class.java).setLifecycleOwner(viewLifecycleOwner).build()
        firebaseAdapter= NoteAdapter(options,requireActivity())
        recyclerViewSetUp()
    }
    private var mContext: Context?=null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext=context
    }

    override fun onDetach() {
        super.onDetach()
        mContext=null
    }

}