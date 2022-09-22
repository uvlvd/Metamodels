
Foo = {
	bar = {
		baz = 41,
		blub = function() end		
	}
}

baz = "blub"

function Foo.bar.blub2(arg1) 
	local ichBinLocal = 42 + arg1
end


function blub3() 
	local ichBinLocal = 43
end

-- This should resolve to 42
Foo.result = Foo.bar.baz

foo = Foo.result



bla = Foo.bar.blub

Foo.bar.blub2()






-------
do
	local ichBinImBlock = 42
	ichBinNichtImBlock = 43
end
result = ichBinImBlock
result = ichBinNichtImBlock





















