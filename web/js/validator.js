function validateLogin() {
    var s = document.getElementById("surname").value;
    var n = document.getElementById("name").value;
    var p = document.getElementById("password").value;

    if (s.length <= 0) {
        alert("Please enter your surname");
        return false;
    }
    if (n.length <= 0) {
        alert("Please enter your name");
        return false;
    }
    if (p.length <= 0) {
        alert("Please enter you password");
        return false;
    }
    return true;
}

function validateAuthor() {
    var a = document.getElementById("author").value;
    if (a.length > 0 && a.includes(" ")) {
        alert("Please enter only the author surname, without any spaces");
        return false;
    }
    return true;
}

