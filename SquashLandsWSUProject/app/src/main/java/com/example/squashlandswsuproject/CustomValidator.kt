package com.example.squashlandswsuproject

class CustomValidator(){
    companion object{
        fun ValidateRequest(request: Request): String{
            if(request.songName.isEmpty()){
                return "Song name can not be left empty"
            }else if(request.songName.contains(Regex("[\$%^&*()_+|~=`{}\\[\\]:\";<>,/]"))){
                return "Song name must not contain any special character"
            }else if(request.patronName.isEmpty() ||  request.patronName.matches(Regex("\\s"))){
                return "Name can not be left empty"
            }else if(request.patronName.contains(Regex("[\$%^&*()_+|~=`{}\\[\\]:\";<>,/]"))){
                return "Name must not contain any special character"
            }else if(request.email.isEmpty()){
                return "Email can not be left empty"
            }else if(!request.email.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+[.][a-zA-Z]{2,}"))){
                return "Email is not in correct format"
            }
            return "successful"
        }
    }
}