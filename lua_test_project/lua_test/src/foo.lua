
local function localFunc()
	return "localFunc"
end


Masked = {
	globalFunc = function(...)
	end,
	localFunc = function(...)
	end
}
function Masked.globalFunc()
	return "globalFunc"
end
function globalFunc()
end


local localVar = "foo"
globalVar = "bar"


result = globalVar
result = Masked.globalFunc()
result = localFunc()
result = localVar

bar = "blu"

Foo = {
	bar = "bar"
}

Foo.aresult = globalVar
Foo.aresult = localFunc()
Foo.aresult = localVar

foo = result


blub = Foo.bar
inFoo = Foo.aresult






