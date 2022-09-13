
local function localFunc()
	return "localFunc"
end


Foo = {}
function Foo.globalFunc()
	return "globalFunc"
end

local localVar = "foo"
globalVar = "bar"


result = globalVar
result = Foo.globalFunc()
result = localFunc()
result = localVar

Foo = {
	bar = "bar"
}
Foo.result = globalVar
--Foo.result = Foo.globalFunc()
Foo.result = localFunc()
Foo.result = localVar

Foo.globalFunc()
localFunc()


blub = Foo.bar