package com.project.notepadapp.Models



data class NoteModel (
    var id:String,
    var title : String,
    var content :String,
    var date: Long,
    var color:Int
)
{




    constructor():this(
        "",
        "",
        "",
        0,
        0

    )
}